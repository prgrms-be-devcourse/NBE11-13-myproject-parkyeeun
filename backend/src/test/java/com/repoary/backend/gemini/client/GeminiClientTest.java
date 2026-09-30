package com.repoary.backend.gemini.client;

import com.repoary.backend.common.exception.ExternalSystemException;
import com.repoary.backend.gemini.exception.GeminiErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class GeminiClientTest {

    private static final String API_KEY = "sensitive-api-key";

    private MockRestServiceServer server;
    private GeminiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://gemini.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiClient(builder);
    }

    @Test
    @DisplayName("Gemini 응답에서 생성된 텍스트를 추출한다")
    void generateText() {
        server.expect(requestTo(
                        "https://gemini.test/v1beta/models/gemini-3.5-flash-lite:generateContent"
                ))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andRespond(withSuccess(
                        """
                                {
                                  "candidates": [{
                                    "content": {
                                      "parts": [
                                        {"text": "# TIL\\n"},
                                        {"text": "배운 내용"}
                                      ]
                                    }
                                  }]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        assertThat(client.generateText(API_KEY, "system", "prompt"))
                .isEqualTo("# TIL\n배운 내용");
        server.verify();
    }

    @Test
    @DisplayName("401 응답을 인증 실패로 매핑한다")
    void authenticationFailure() {
        assertStatusMapping(
                HttpStatus.UNAUTHORIZED,
                GeminiErrorCode.AUTHENTICATION_FAILED
        );
    }

    @Test
    @DisplayName("400 API_KEY_INVALID 응답을 인증 실패로 매핑하고 민감정보를 로그에서 제외한다")
    void invalidApiKeyReasonIsAuthenticationFailure(
            CapturedOutput output
    ) {
        server.expect(requestTo(
                        "https://gemini.test/v1beta/models/gemini-3.5-flash-lite:generateContent"
                ))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": {
                                    "code": 400,
                                    "status": "INVALID_ARGUMENT",
                                    "message": "API key not valid: sensitive-api-key",
                                    "details": [{
                                      "@type": "type.googleapis.com/google.rpc.ErrorInfo",
                                      "reason": "API_KEY_INVALID",
                                      "domain": "googleapis.com",
                                      "metadata": {
                                        "service": "generativelanguage.googleapis.com",
                                        "sensitive": "private-metadata"
                                      }
                                    }]
                                  }
                                }
                                """));

        assertErrorCode(
                () -> client.generateText(
                        API_KEY,
                        "sensitive-system-instruction",
                        "private-prompt-source"
                ),
                GeminiErrorCode.AUTHENTICATION_FAILED
        );

        assertThat(output.getOut())
                .contains(
                        "Gemini API request failed. status=400, "
                                + "errorStatus=INVALID_ARGUMENT, errorCode=400"
                )
                .doesNotContain(
                        API_KEY,
                        "API_KEY_INVALID",
                        "sensitive-system-instruction",
                        "private-prompt-source",
                        "private-metadata",
                        "generativelanguage.googleapis.com"
                );
        server.verify();
    }

    @Test
    @DisplayName("400 INVALID_ARGUMENT에 인증 reason이 없으면 일반 API 오류로 매핑한다")
    void invalidRequestIsNotAuthenticationFailure() {
        server.expect(requestTo(
                        "https://gemini.test/v1beta/models/gemini-3.5-flash-lite:generateContent"
                ))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": {
                                    "code": 400,
                                    "status": "INVALID_ARGUMENT",
                                    "message": "Request contains an invalid argument."
                                  }
                                }
                                """));

        assertErrorCode(
                () -> client.generateText(API_KEY, "system", "prompt"),
                GeminiErrorCode.API_ERROR
        );
        server.verify();
    }

    @Test
    @DisplayName("429 응답을 호출 한도 초과로 매핑한다")
    void rateLimit() {
        assertStatusMapping(
                HttpStatus.TOO_MANY_REQUESTS,
                GeminiErrorCode.RATE_LIMIT_EXCEEDED
        );
    }

    @Test
    @DisplayName("Gemini 서버 오류를 API 오류로 매핑한다")
    void apiError() {
        assertStatusMapping(
                HttpStatus.INTERNAL_SERVER_ERROR,
                GeminiErrorCode.API_ERROR
        );
    }

    @Test
    @DisplayName("Gemini 오류에서 안전한 status와 code만 로그에 남긴다")
    void apiErrorLogsOnlySafeMetadata(CapturedOutput output) {
        server.expect(requestTo(
                        "https://gemini.test/v1beta/models/gemini-3.5-flash-lite:generateContent"
                ))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": {
                                    "code": 400,
                                    "status": "INVALID_ARGUMENT",
                                    "message": "sensitive-google-message private-source"
                                  }
                                }
                                """));

        assertErrorCode(
                () -> client.generateText(
                        API_KEY,
                        "sensitive-system-instruction",
                        "private-prompt-source"
                ),
                GeminiErrorCode.API_ERROR
        );

        assertThat(output.getOut())
                .contains(
                        "Gemini API request failed. status=400, "
                                + "errorStatus=INVALID_ARGUMENT, errorCode=400"
                )
                .doesNotContain(
                        API_KEY,
                        "sensitive-system-instruction",
                        "private-prompt-source",
                        "sensitive-google-message"
                );
        server.verify();
    }

    @Test
    @DisplayName("503 UNAVAILABLE 후 성공하면 backoff 후 결과를 반환한다")
    void unavailableThenSuccess(CapturedOutput output) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://gemini-retry.test");
        MockRestServiceServer retryServer =
                MockRestServiceServer.bindTo(builder).build();
        List<Long> delays = new ArrayList<>();
        GeminiClient retryClient = new GeminiClient(
                builder,
                delays::add,
                () -> 0L
        );

        expectUnavailable(retryServer);
        retryServer.expect(requestTo(
                        "https://gemini-retry.test/v1beta/models/gemini-3.5-flash-lite:generateContent"
                ))
                .andRespond(withSuccess(
                        """
                                {
                                  "candidates": [{
                                    "content": {"parts": [{"text": "# TIL"}]}
                                  }]
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        assertThat(retryClient.generateText(API_KEY, "system", "prompt"))
                .isEqualTo("# TIL");
        assertThat(delays).containsExactly(1_000L);
        assertThat(output.getOut())
                .doesNotContain("Gemini API request failed. status=503");
        retryServer.verify();
    }

    @Test
    @DisplayName("503 UNAVAILABLE이 계속되면 세 번 재시도 후 실패한다")
    void unavailableRetriesAtMostThreeTimes(CapturedOutput output) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://gemini-retry.test");
        MockRestServiceServer retryServer =
                MockRestServiceServer.bindTo(builder).build();
        List<Long> delays = new ArrayList<>();
        GeminiClient retryClient = new GeminiClient(
                builder,
                delays::add,
                () -> 0L
        );

        for (int attempt = 0; attempt < 4; attempt++) {
            expectUnavailable(retryServer);
        }

        assertErrorCode(
                () -> retryClient.generateText(API_KEY, "system", "prompt"),
                GeminiErrorCode.API_ERROR
        );

        assertThat(delays).containsExactly(1_000L, 2_000L, 4_000L);
        assertThat(countOccurrences(
                output.getOut(),
                "Gemini API request failed. status=503, "
                        + "errorStatus=UNAVAILABLE, errorCode=503"
        )).isEqualTo(1);
        retryServer.verify();
    }

    @Test
    @DisplayName("텍스트가 없는 응답을 잘못된 응답으로 처리한다")
    void missingText() {
        server.expect(requestTo(
                        "https://gemini.test/v1beta/models/gemini-3.5-flash-lite:generateContent"
                ))
                .andRespond(withSuccess(
                        "{\"candidates\":[{\"content\":{\"parts\":[]}}]}",
                        MediaType.APPLICATION_JSON
                ));

        assertErrorCode(
                () -> client.generateText(API_KEY, "system", "prompt"),
                GeminiErrorCode.INVALID_RESPONSE
        );
    }

    @Test
    @DisplayName("timeout과 connection failure를 구분하고 key를 예외에 노출하지 않는다")
    void transportErrorsDoNotExposeApiKey() {
        GeminiClient timeoutClient = clientThatThrows(
                new ResourceAccessException(
                        "timeout",
                        new SocketTimeoutException("timeout")
                )
        );
        GeminiClient connectionClient = clientThatThrows(
                new ResourceAccessException(
                        "connection",
                        new ConnectException("connection")
                )
        );

        assertErrorCode(
                () -> timeoutClient.generateText(API_KEY, "system", "prompt"),
                GeminiErrorCode.TIMEOUT
        );
        assertErrorCode(
                () -> connectionClient.generateText(API_KEY, "system", "prompt"),
                GeminiErrorCode.CONNECTION_FAILED
        );
    }

    private void assertStatusMapping(
            HttpStatus status,
            GeminiErrorCode expected
    ) {
        server.expect(requestTo(
                        "https://gemini.test/v1beta/models/gemini-3.5-flash-lite:generateContent"
                ))
                .andRespond(withStatus(status));

        assertErrorCode(
                () -> client.generateText(API_KEY, "system", "prompt"),
                expected
        );
        server.verify();
    }

    private void expectUnavailable(MockRestServiceServer retryServer) {
        retryServer.expect(requestTo(
                        "https://gemini-retry.test/v1beta/models/gemini-3.5-flash-lite:generateContent"
                ))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": {
                                    "code": 503,
                                    "status": "UNAVAILABLE",
                                    "message": "temporary overload"
                                  }
                                }
                                """));
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }

    private void assertErrorCode(
            ThrowingCall call,
            GeminiErrorCode expected
    ) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        ExternalSystemException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(expected);
                            assertThat(exception.getMessage())
                                    .doesNotContain(API_KEY);
                        }
                );
    }

    private GeminiClient clientThatThrows(ResourceAccessException failure) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://gemini.test")
                .requestInterceptor((request, body, execution) -> {
                    throw failure;
                });
        return new GeminiClient(builder);
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }
}
