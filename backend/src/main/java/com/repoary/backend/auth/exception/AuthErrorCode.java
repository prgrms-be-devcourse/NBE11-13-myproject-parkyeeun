package com.repoary.backend.auth.exception;

import com.repoary.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_AUTHENTICATION_REQUIRED", "인증이 필요합니다."),
    INVALID_AUTHORIZATION_HEADER(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_AUTHORIZATION_HEADER", "Authorization 헤더 형식이 올바르지 않습니다."),
    JWT_MALFORMED(HttpStatus.UNAUTHORIZED, "AUTH_JWT_MALFORMED", "JWT 형식이 올바르지 않습니다."),
    JWT_INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "AUTH_JWT_INVALID_SIGNATURE", "JWT 서명이 유효하지 않습니다."),
    JWT_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_JWT_EXPIRED", "JWT가 만료되었습니다."),
    JWT_INVALID_PAYLOAD(HttpStatus.UNAUTHORIZED, "AUTH_JWT_INVALID_PAYLOAD", "JWT payload가 올바르지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_ACCESS_DENIED", "접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
