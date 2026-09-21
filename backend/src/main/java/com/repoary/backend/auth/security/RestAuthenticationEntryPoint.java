package com.repoary.backend.auth.security;

import com.repoary.backend.auth.exception.AuthErrorCode;
import com.repoary.backend.auth.exception.JwtAuthenticationException;
import com.repoary.backend.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter responseWriter;

    public RestAuthenticationEntryPoint(
            SecurityErrorResponseWriter responseWriter
    ) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        ErrorCode errorCode = exception instanceof JwtAuthenticationException jwtException
                ? jwtException.getErrorCode()
                : AuthErrorCode.AUTHENTICATION_REQUIRED;
        responseWriter.write(response, errorCode);
    }
}
