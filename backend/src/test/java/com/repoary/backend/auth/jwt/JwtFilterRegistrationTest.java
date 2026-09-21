package com.repoary.backend.auth.jwt;

import com.repoary.backend.auth.security.RestAccessDeniedHandler;
import com.repoary.backend.auth.security.RestAuthenticationEntryPoint;
import com.repoary.backend.common.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtFilterRegistrationTest {

    @Test
    @DisplayName("JWT 필터의 서블릿 컨테이너 자동 등록을 비활성화한다")
    void servletFilterRegistrationIsDisabled() {
        JwtAuthenticationFilter jwtFilter =
                mock(JwtAuthenticationFilter.class);
        SecurityConfig securityConfig = new SecurityConfig(
                jwtFilter,
                mock(RestAuthenticationEntryPoint.class),
                mock(RestAccessDeniedHandler.class)
        );

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                securityConfig.jwtAuthenticationFilterRegistration();

        assertThat(registration.getFilter()).isSameAs(jwtFilter);
        assertThat(registration.isEnabled()).isFalse();
    }
}
