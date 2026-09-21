package com.repoary.backend.auth.jwt;

import com.repoary.backend.auth.exception.AuthErrorCode;
import com.repoary.backend.auth.exception.JwtAuthenticationException;
import com.repoary.backend.auth.exception.JwtSystemException;
import com.repoary.backend.auth.security.RestAuthenticationEntryPoint;
import com.repoary.backend.auth.security.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JsonMapper jsonMapper;
    private JwtProvider jwtProvider;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        jwtProvider = mock(JwtProvider.class);
        SecurityErrorResponseWriter responseWriter =
                new SecurityErrorResponseWriter(jsonMapper);
        filter = new JwtAuthenticationFilter(
                jwtProvider,
                new RestAuthenticationEntryPoint(responseWriter),
                responseWriter
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 다음 필터로 진행한다")
    void missingAuthorizationHeaderContinues() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtProvider, never()).getUserId("token");
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 안전한 401 응답을 작성한다")
    void malformedAuthorizationHeaderReturnsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic secret-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        JsonNode body = jsonMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText())
                .isEqualTo("AUTH_INVALID_AUTHORIZATION_HEADER");
        assertThat(response.getContentAsString())
                .doesNotContain("secret-value");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("JWT 내부 처리 오류는 안전한 500 응답을 작성한다")
    void jwtSystemErrorReturnsInternalServerError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        when(jwtProvider.getUserId("token"))
                .thenThrow(new JwtSystemException(
                        new IllegalStateException("internal")
                ));

        filter.doFilter(request, response, filterChain);

        JsonNode body = jsonMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(body.get("code").asText())
                .isEqualTo("COMMON_INTERNAL_SERVER_ERROR");
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("후속 필터의 JWT 예외를 인증 필터가 가로채지 않는다")
    void downstreamJwtExceptionIsPropagated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        JwtAuthenticationException downstreamException =
                new JwtAuthenticationException(AuthErrorCode.JWT_EXPIRED);
        when(jwtProvider.getUserId("valid-token")).thenReturn(7L);
        doThrow(downstreamException)
                .when(filterChain)
                .doFilter(request, response);

        assertThatThrownBy(() ->
                filter.doFilter(request, response, filterChain)
        ).isSameAs(downstreamException);

        assertThat(response.getContentAsByteArray()).isEmpty();
        assertThat(SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal()).isEqualTo(7L);
    }
}
