package com.repoary.backend.auth.jwt;

import com.repoary.backend.auth.exception.AuthErrorCode;
import com.repoary.backend.auth.exception.JwtAuthenticationException;
import com.repoary.backend.auth.exception.JwtSystemException;
import com.repoary.backend.auth.security.RestAuthenticationEntryPoint;
import com.repoary.backend.auth.security.SecurityErrorResponseWriter;
import com.repoary.backend.common.exception.CommonErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(
            JwtAuthenticationFilter.class
    );
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final SecurityErrorResponseWriter responseWriter;

    public JwtAuthenticationFilter(
            JwtProvider jwtProvider,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            SecurityErrorResponseWriter responseWriter
    ) {
        this.jwtProvider = jwtProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.responseWriter = responseWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authorizationHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticate(authorizationHeader);
        } catch (JwtAuthenticationException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    exception
            );
            return;
        } catch (JwtSystemException exception) {
            SecurityContextHolder.clearContext();
            log.error(
                    "JWT processing failed due to an internal error. causeType={}",
                    exception.getCause() == null
                            ? "unknown"
                            : exception.getCause().getClass().getName()
            );
            responseWriter.write(
                    response,
                    CommonErrorCode.INTERNAL_SERVER_ERROR
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String authorizationHeader) {
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new JwtAuthenticationException(
                    AuthErrorCode.INVALID_AUTHORIZATION_HEADER
            );
        }

        String token = authorizationHeader
                .substring(BEARER_PREFIX.length())
                .trim();
        if (token.isBlank()) {
            throw new JwtAuthenticationException(
                    AuthErrorCode.INVALID_AUTHORIZATION_HEADER
            );
        }

        Long userId = jwtProvider.getUserId(token);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of()
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
