package com.repoary.backend.auth.security;

import com.repoary.backend.auth.exception.AuthErrorCode;
import com.repoary.backend.auth.exception.JwtAuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHandlerTest {

    private JsonMapper jsonMapper;
    private RestAuthenticationEntryPoint authenticationEntryPoint;
    private RestAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        SecurityErrorResponseWriter responseWriter =
                new SecurityErrorResponseWriter(jsonMapper);
        authenticationEntryPoint =
                new RestAuthenticationEntryPoint(responseWriter);
        accessDeniedHandler = new RestAccessDeniedHandler(responseWriter);
    }

    @Test
    @DisplayName("인증 정보가 없으면 공통 형식의 401을 응답한다")
    void authenticationRequired() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new InsufficientAuthenticationException("internal")
        );

        JsonNode body = jsonMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText())
                .isEqualTo("AUTH_AUTHENTICATION_REQUIRED");
        assertThat(body.get("message").asText())
                .isEqualTo("인증이 필요합니다.");
    }

    @Test
    @DisplayName("JWT 인증 실패의 세부 코드를 유지한다")
    void jwtAuthenticationFailure() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                new MockHttpServletRequest(),
                response,
                new JwtAuthenticationException(AuthErrorCode.JWT_EXPIRED)
        );

        JsonNode body = jsonMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText())
                .isEqualTo("AUTH_JWT_EXPIRED");
    }

    @Test
    @DisplayName("인가 실패는 공통 형식의 403을 응답한다")
    void accessDenied() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("internal")
        );

        JsonNode body = jsonMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("code").asText())
                .isEqualTo("AUTH_ACCESS_DENIED");
    }
}
