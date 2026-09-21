package com.repoary.backend.auth.client;

import com.repoary.backend.auth.dto.GitHubAccessTokenRequest;
import com.repoary.backend.common.exception.ExternalSystemException;
import com.repoary.backend.github.exception.GitHubErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubOAuthClientTest {

    @Test
    @DisplayName("OAuth 토큰 요청의 읽기 타임아웃은 TIMEOUT으로 분류한다")
    void tokenRequestTimeoutIsClassifiedAsTimeout() {
        GitHubOAuthClient client = new GitHubOAuthClient(
                builderThatThrows(new ResourceAccessException(
                        "read timed out",
                        new SocketTimeoutException("secret endpoint")
                )),
                RestClient.builder()
        );

        GitHubAccessTokenRequest request = new GitHubAccessTokenRequest(
                "client-id",
                "client-secret",
                "code",
                "https://repoary.test/callback"
        );

        assertThatThrownBy(() -> client.requestAccessToken(request))
                .isInstanceOfSatisfying(
                        ExternalSystemException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(GitHubErrorCode.TIMEOUT)
                );
    }

    @Test
    @DisplayName("GitHub 사용자 조회의 연결 실패는 CONNECTION_FAILED로 분류한다")
    void userRequestConnectionFailureIsClassifiedAsConnectionFailed() {
        GitHubOAuthClient client = new GitHubOAuthClient(
                RestClient.builder(),
                builderThatThrows(new ResourceAccessException(
                        "connection failed",
                        new ConnectException("secret endpoint")
                ))
        );

        assertThatThrownBy(() -> client.requestUserInfo("token"))
                .isInstanceOfSatisfying(
                        ExternalSystemException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(GitHubErrorCode.CONNECTION_FAILED)
                );
    }

    private RestClient.Builder builderThatThrows(
            ResourceAccessException failure
    ) {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    throw failure;
                });
    }
}
