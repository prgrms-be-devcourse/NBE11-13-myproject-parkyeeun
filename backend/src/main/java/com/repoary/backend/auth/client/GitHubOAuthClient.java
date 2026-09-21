package com.repoary.backend.auth.client;

import com.repoary.backend.auth.dto.GitHubAccessTokenRequest;
import com.repoary.backend.auth.dto.GitHubAccessTokenResponse;
import com.repoary.backend.auth.dto.GitHubUserResponse;
import com.repoary.backend.common.exception.ExternalSystemException;
import com.repoary.backend.github.exception.GitHubErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.function.Supplier;

@Component
public class GitHubOAuthClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final RestClient apiClient;

    public GitHubOAuthClient() {
        this(createBuilder(), createBuilder());
    }

    GitHubOAuthClient(
            RestClient.Builder oauthBuilder,
            RestClient.Builder apiBuilder
    ) {
        this.restClient = createClient(
                oauthBuilder,
                "https://github.com"
        );
        this.apiClient = createClient(
                apiBuilder,
                "https://api.github.com"
        );
    }

    public GitHubAccessTokenResponse requestAccessToken(GitHubAccessTokenRequest request) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", request.clientId());
        formData.add("client_secret", request.clientSecret());
        formData.add("code", request.code());
        formData.add("redirect_uri", request.redirectUri());

        GitHubAccessTokenResponse response = execute(() -> restClient.post()
                .uri("/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Accept", "application/json")
                .body(formData)
                .retrieve()
                .body(GitHubAccessTokenResponse.class));

        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()) {
            throw new ExternalSystemException(
                    GitHubErrorCode.AUTHENTICATION_FAILED,
                    null
            );
        }
        return response;
    }

    public GitHubUserResponse requestUserInfo(String accessToken) {
        GitHubUserResponse response = execute(() -> apiClient.get()
                .uri("/user")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(GitHubUserResponse.class));

        if (response == null
                || response.id() == null
                || response.login() == null
                || response.login().isBlank()) {
            throw new ExternalSystemException(
                    GitHubErrorCode.INVALID_RESPONSE,
                    null
            );
        }
        return response;
    }

    private static RestClient.Builder createBuilder() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    private RestClient createClient(
            RestClient.Builder builder,
            String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        GitHubOAuthClient::handleErrorStatus
                )
                .build();
    }

    private <T> T execute(Supplier<T> request) {
        try {
            return request.get();
        } catch (ExternalSystemException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new ExternalSystemException(
                    hasTimeoutCause(exception)
                            ? GitHubErrorCode.TIMEOUT
                            : GitHubErrorCode.CONNECTION_FAILED,
                    exception
            );
        } catch (RestClientException exception) {
            throw new ExternalSystemException(
                    GitHubErrorCode.INVALID_RESPONSE,
                    exception
            );
        }
    }

    private static void handleErrorStatus(
            org.springframework.http.HttpRequest request,
            org.springframework.http.client.ClientHttpResponse response
    ) throws IOException {
        int status = response.getStatusCode().value();
        GitHubErrorCode errorCode;

        if (status == 401) {
            errorCode = GitHubErrorCode.AUTHENTICATION_FAILED;
        } else if (status == 403) {
            errorCode = "0".equals(response.getHeaders().getFirst("X-RateLimit-Remaining"))
                    || response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER) != null
                    ? GitHubErrorCode.RATE_LIMIT_EXCEEDED
                    : GitHubErrorCode.ACCESS_DENIED;
        } else if (status == 404) {
            errorCode = GitHubErrorCode.RESOURCE_NOT_FOUND;
        } else if (status == 429) {
            errorCode = GitHubErrorCode.RATE_LIMIT_EXCEEDED;
        } else if (status >= 500) {
            errorCode = GitHubErrorCode.API_UNAVAILABLE;
        } else {
            errorCode = GitHubErrorCode.AUTHENTICATION_FAILED;
        }
        throw new ExternalSystemException(errorCode, null);
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
