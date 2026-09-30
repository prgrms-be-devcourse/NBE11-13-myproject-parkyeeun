package com.repoary.backend.gemini.client;

import com.repoary.backend.common.exception.ExternalSystemException;
import com.repoary.backend.gemini.dto.GeminiGenerateRequest;
import com.repoary.backend.gemini.dto.GeminiGenerateResponse;
import com.repoary.backend.gemini.exception.GeminiErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(
            GeminiClient.class
    );
    private static final JsonMapper ERROR_JSON_MAPPER =
            JsonMapper.builder().build();
    private static final Pattern SAFE_ERROR_STATUS =
            Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private static final Pattern SAFE_ERROR_REASON =
            Pattern.compile("[A-Z][A-Z0-9_]{0,127}");
    private static final String MODEL = "gemini-3.5-flash-lite";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(45);
    private static final int MAX_UNAVAILABLE_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MILLIS = {
            1_000L,
            2_000L,
            4_000L
    };
    private static final long MAX_JITTER_MILLIS = 250L;

    private final RestClient restClient;
    private final Sleeper sleeper;
    private final LongSupplier jitterSupplier;

    public GeminiClient() {
        this(createBuilder().baseUrl(
                "https://generativelanguage.googleapis.com"
        ));
    }

    private static RestClient.Builder createBuilder() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory);
    }

    GeminiClient(RestClient.Builder builder) {
        this(
                builder,
                Thread::sleep,
                () -> ThreadLocalRandom.current().nextLong(
                        MAX_JITTER_MILLIS + 1
                )
        );
    }

    GeminiClient(
            RestClient.Builder builder,
            Sleeper sleeper,
            LongSupplier jitterSupplier
    ) {
        this.restClient = builder
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        GeminiClient::handleErrorStatus
                )
                .build();
        this.sleeper = sleeper;
        this.jitterSupplier = jitterSupplier;
    }

    public String generateText(
            String apiKey,
            String systemInstruction,
            String prompt
    ) {
        GeminiGenerateResponse response;

        try {
            response = requestWithRetry(
                    apiKey,
                    systemInstruction,
                    prompt
            );
        } catch (ExternalSystemException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            GeminiErrorCode errorCode = hasTimeoutCause(exception)
                    ? GeminiErrorCode.TIMEOUT
                    : GeminiErrorCode.CONNECTION_FAILED;
            throw new ExternalSystemException(errorCode, exception);
        } catch (RestClientException exception) {
            throw new ExternalSystemException(
                    GeminiErrorCode.INVALID_RESPONSE,
                    exception
            );
        }

        return extractText(response);
    }

    private GeminiGenerateResponse requestWithRetry(
            String apiKey,
            String systemInstruction,
            String prompt
    ) {
        int retryCount = 0;

        while (true) {
            try {
                return restClient.post()
                        .uri("/v1beta/models/{model}:generateContent", MODEL)
                        .header("x-goog-api-key", apiKey)
                        .body(GeminiGenerateRequest.of(
                                systemInstruction,
                                prompt
                        ))
                        .retrieve()
                        .body(GeminiGenerateResponse.class);
            } catch (GeminiHttpException exception) {
                if (!exception.isUnavailable()
                        || retryCount >= MAX_UNAVAILABLE_RETRIES) {
                    logFinalHttpFailure(exception);
                    throw new ExternalSystemException(
                            exception.errorCode(),
                            null
                    );
                }

                try {
                    sleeper.sleep(retryDelayMillis(retryCount));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    logFinalHttpFailure(exception);
                    throw new ExternalSystemException(
                            GeminiErrorCode.API_ERROR,
                            interruptedException
                    );
                }
                retryCount++;
            }
        }
    }

    private long retryDelayMillis(int retryCount) {
        long jitter = Math.max(
                0L,
                Math.min(MAX_JITTER_MILLIS, jitterSupplier.getAsLong())
        );
        return RETRY_DELAYS_MILLIS[retryCount] + jitter;
    }

    private void logFinalHttpFailure(GeminiHttpException exception) {
        log.warn(
                "Gemini API request failed. status={}, errorStatus={}, errorCode={}",
                exception.status(),
                exception.errorDetails().status(),
                exception.errorDetails().code()
        );
    }

    private String extractText(GeminiGenerateResponse response) {
        if (response == null || response.candidates() == null) {
            throw invalidResponse();
        }

        return response.candidates().stream()
                .filter(candidate -> candidate != null
                        && candidate.content() != null
                        && candidate.content().parts() != null)
                .map(candidate -> joinText(candidate.content().parts()))
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElseThrow(this::invalidResponse);
    }

    private String joinText(List<GeminiGenerateResponse.Part> parts) {
        return parts.stream()
                .filter(part -> part != null && part.text() != null)
                .map(GeminiGenerateResponse.Part::text)
                .filter(text -> !text.isBlank())
                .reduce("", (left, right) -> left + right)
                .trim();
    }

    private ExternalSystemException invalidResponse() {
        return new ExternalSystemException(
                GeminiErrorCode.INVALID_RESPONSE,
                null
        );
    }

    private static void handleErrorStatus(
            org.springframework.http.HttpRequest request,
            org.springframework.http.client.ClientHttpResponse response
    ) throws IOException {
        int status = response.getStatusCode().value();
        SafeErrorDetails errorDetails = readSafeErrorDetails(response);
        GeminiErrorCode errorCode = resolveErrorCode(
                status,
                errorDetails
        );

        throw new GeminiHttpException(
                status,
                errorDetails,
                errorCode
        );
    }

    private static GeminiErrorCode resolveErrorCode(
            int httpStatus,
            SafeErrorDetails errorDetails
    ) {
        if (httpStatus == 401
                || httpStatus == 403
                || "UNAUTHENTICATED".equals(errorDetails.status())
                || errorDetails.reason().startsWith("API_KEY_")) {
            return GeminiErrorCode.AUTHENTICATION_FAILED;
        }
        if (httpStatus == 429
                || "RESOURCE_EXHAUSTED".equals(errorDetails.status())) {
            return GeminiErrorCode.RATE_LIMIT_EXCEEDED;
        }
        return GeminiErrorCode.API_ERROR;
    }

    private static SafeErrorDetails readSafeErrorDetails(
            org.springframework.http.client.ClientHttpResponse response
    ) {
        try {
            GeminiErrorResponse body = ERROR_JSON_MAPPER.readValue(
                    response.getBody(),
                    GeminiErrorResponse.class
            );
            if (body == null || body.error() == null) {
                return SafeErrorDetails.unknown();
            }

            String errorStatus = body.error().status();
            String safeStatus = errorStatus != null
                    && SAFE_ERROR_STATUS.matcher(errorStatus).matches()
                    ? errorStatus
                    : "unknown";
            Integer errorCode = body.error().code();
            String safeReason = body.error().details() == null
                    ? "unknown"
                    : body.error().details().stream()
                    .filter(detail -> detail != null
                            && detail.reason() != null
                            && SAFE_ERROR_REASON
                            .matcher(detail.reason())
                            .matches())
                    .map(GeminiErrorDetail::reason)
                    .findFirst()
                    .orElse("unknown");

            return new SafeErrorDetails(
                    safeStatus,
                    errorCode == null ? "unknown" : errorCode.toString(),
                    safeReason
            );
        } catch (Exception ignored) {
            return SafeErrorDetails.unknown();
        }
    }

    private record GeminiErrorResponse(GeminiError error) {
    }

    private record GeminiError(
            Integer code,
            String status,
            List<GeminiErrorDetail> details
    ) {
    }

    private record GeminiErrorDetail(String reason) {
    }

    private record SafeErrorDetails(
            String status,
            String code,
            String reason
    ) {
        private static SafeErrorDetails unknown() {
            return new SafeErrorDetails(
                    "unknown",
                    "unknown",
                    "unknown"
            );
        }
    }

    private static final class GeminiHttpException
            extends RuntimeException {

        private final int status;
        private final SafeErrorDetails errorDetails;
        private final GeminiErrorCode errorCode;

        private GeminiHttpException(
                int status,
                SafeErrorDetails errorDetails,
                GeminiErrorCode errorCode
        ) {
            super(errorCode.getMessage());
            this.status = status;
            this.errorDetails = errorDetails;
            this.errorCode = errorCode;
        }

        private int status() {
            return status;
        }

        private SafeErrorDetails errorDetails() {
            return errorDetails;
        }

        private GeminiErrorCode errorCode() {
            return errorCode;
        }

        private boolean isUnavailable() {
            return status == 503
                    && "UNAVAILABLE".equals(errorDetails.status());
        }
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
