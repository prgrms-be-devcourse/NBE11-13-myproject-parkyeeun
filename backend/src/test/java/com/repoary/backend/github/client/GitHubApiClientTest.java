package com.repoary.backend.github.client;

import com.repoary.backend.common.exception.ExternalSystemException;
import com.repoary.backend.github.exception.GitHubErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubApiClientTest {

    private MockRestServiceServer server;
    private GitHubApiClient gitHubApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.github.test");
        server = MockRestServiceServer.bindTo(builder).build();
        gitHubApiClient = new GitHubApiClient(builder);
    }

    @Test
    @DisplayName("콘텐츠 404는 기존 동작대로 Optional.empty로 처리한다")
    void contentNotFoundReturnsEmpty() {
        server.expect(requestTo(
                        "https://api.github.test/repos/owner/repository/contents/TIL/2026-09-21.md?ref=main"
                ))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(gitHubApiClient.getContent(
                "token",
                "owner",
                "repository",
                "main",
                "TIL/2026-09-21.md"
        )).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("성공 상태의 빈 콘텐츠 응답은 404로 오인하지 않는다")
    void emptyContentResponseIsInvalidResponse() {
        server.expect(requestTo(
                        "https://api.github.test/repos/owner/repository/contents/TIL/2026-09-21.md?ref=main"
                ))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> gitHubApiClient.getContent(
                "token",
                "owner",
                "repository",
                "main",
                "TIL/2026-09-21.md"
        )).isInstanceOfSatisfying(
                ExternalSystemException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(GitHubErrorCode.INVALID_RESPONSE)
        );
    }

    @Test
    @DisplayName("GitHub 403과 rate-limit 헤더를 429 오류로 구분한다")
    void rateLimitResponseIsDistinguishedFromForbidden() {
        server.expect(requestTo("https://api.github.test/user/repos?sort=updated&per_page=100"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .header("X-RateLimit-Remaining", "0"));

        assertThatThrownBy(() -> gitHubApiClient.getRepositories("token"))
                .isInstanceOfSatisfying(
                        ExternalSystemException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(GitHubErrorCode.RATE_LIMIT_EXCEEDED)
                );
    }

    @Test
    @DisplayName("rate-limit 근거가 없는 GitHub 403은 권한 부족으로 처리한다")
    void forbiddenResponseIsAccessDenied() {
        server.expect(requestTo("https://api.github.test/user/repos?sort=updated&per_page=100"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> gitHubApiClient.getRepositories("token"))
                .isInstanceOfSatisfying(
                        ExternalSystemException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(GitHubErrorCode.ACCESS_DENIED)
                );
    }

    @Test
    @DisplayName("커밋 응답의 필수 중첩 필드가 누락되면 잘못된 외부 응답으로 처리한다")
    void missingNestedCommitDataIsInvalidResponse() {
        server.expect(requestTo(
                        "https://api.github.test/repos/owner/repository/commits?sha=main&since=2026-09-21T00:00:00Z&until=2026-09-22T00:00:00Z&per_page=100&page=1"
                ))
                .andRespond(withSuccess(
                        """
                                [
                                  {
                                    "sha": "abc123",
                                    "html_url": "https://github.test/commit/abc123",
                                    "commit": null
                                  }
                                ]
                                """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> gitHubApiClient.getCommits(
                "token",
                "owner",
                "repository",
                "main",
                Instant.parse("2026-09-21T00:00:00Z"),
                Instant.parse("2026-09-22T00:00:00Z")
        )).isInstanceOfSatisfying(
                ExternalSystemException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(GitHubErrorCode.INVALID_RESPONSE)
        );
    }

    @Test
    @DisplayName("읽기 타임아웃은 GitHub TIMEOUT 오류로 분류한다")
    void readTimeoutIsClassifiedAsTimeout() {
        GitHubApiClient client = clientThatThrows(
                new ResourceAccessException(
                        "read timed out",
                        new SocketTimeoutException("secret endpoint")
                )
        );

        assertThatThrownBy(() -> client.getRepositories("token"))
                .isInstanceOfSatisfying(
                        ExternalSystemException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(GitHubErrorCode.TIMEOUT)
                );
    }

    @Test
    @DisplayName("타임아웃이 아닌 연결 실패는 CONNECTION_FAILED로 분류한다")
    void connectionFailureIsClassifiedAsConnectionFailed() {
        GitHubApiClient client = clientThatThrows(
                new ResourceAccessException(
                        "connection failed",
                        new ConnectException("secret endpoint")
                )
        );

        assertThatThrownBy(() -> client.getRepositories("token"))
                .isInstanceOfSatisfying(
                        ExternalSystemException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(GitHubErrorCode.CONNECTION_FAILED)
                );
    }

    private GitHubApiClient clientThatThrows(
            ResourceAccessException failure
    ) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.github.test")
                .requestInterceptor((request, body, execution) -> {
                    throw failure;
                });

        return new GitHubApiClient(builder);
    }
}
