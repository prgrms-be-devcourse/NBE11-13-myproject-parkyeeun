package com.repoary.backend.common.exception;

import com.repoary.backend.til.exception.TilErrorCode;
import com.repoary.backend.github.exception.GitHubErrorCode;
import com.repoary.backend.repository.exception.RepositoryErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    @DisplayName("비즈니스 예외의 상태, 코드, 안전한 메시지를 응답한다")
    void handleBusinessException() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(
                        new BusinessException(TilErrorCode.NOT_FOUND)
                );

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().code()).isEqualTo("TIL_NOT_FOUND");
        assertThat(response.getBody().message())
                .isEqualTo("TIL을 찾을 수 없습니다.");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("예상하지 못한 예외의 내부 메시지를 응답에 노출하지 않는다")
    void hideUnexpectedExceptionMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpectedException(
                        new IllegalStateException(
                                "GitHub access token=secret-value"
                        )
                );

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("COMMON_INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message())
                .isEqualTo("서버 내부 오류가 발생했습니다.")
                .doesNotContain("secret-value");
    }

    @Test
    @DisplayName("일반 IllegalArgumentException을 비즈니스 오류로 오분류하지 않는다")
    void treatUnexpectedIllegalArgumentAsServerError() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpectedException(
                        new IllegalArgumentException("internal detail")
                );

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().code())
                .isEqualTo("COMMON_INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message())
                .doesNotContain("internal detail");
    }

    @Test
    @DisplayName("잘못된 GitHub 응답은 안전한 502 오류로 변환한다")
    void mapInvalidGitHubResponseToBadGateway() {
        ResponseEntity<ErrorResponse> response =
                handler.handleExternalSystemException(
                        new ExternalSystemException(
                                GitHubErrorCode.INVALID_RESPONSE,
                                new IllegalArgumentException(
                                        "token=secret-value"
                                )
                        )
                );

        assertThat(response.getStatusCode().value()).isEqualTo(502);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("GITHUB_INVALID_RESPONSE");
        assertThat(response.getBody().message())
                .isEqualTo("GitHub API 응답을 처리할 수 없습니다.")
                .doesNotContain("secret-value");
    }

    @Test
    @DisplayName("TIL 중복은 409와 전용 오류 코드를 반환한다")
    void mapDuplicateTilToConflict() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(
                        new BusinessException(TilErrorCode.ALREADY_EXISTS)
                );

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("TIL_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("소유하지 않은 저장소를 숨기는 오류는 404를 반환한다")
    void mapRepositoryNotFoundToNotFound() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessException(
                        new BusinessException(
                                RepositoryErrorCode.CONNECTED_REPOSITORY_NOT_FOUND
                        )
                );

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("REPOSITORY_NOT_FOUND");
    }

}
