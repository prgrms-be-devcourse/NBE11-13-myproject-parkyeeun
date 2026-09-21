package com.repoary.backend.github.exception;

import com.repoary.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GitHubErrorCode implements ErrorCode {

    ACCESS_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "GITHUB_ACCESS_TOKEN_MISSING", "GitHub 인증 정보가 없습니다."),
    COMMIT_SHA_REQUIRED(HttpStatus.BAD_REQUEST, "GITHUB_COMMIT_SHA_REQUIRED", "커밋 SHA는 필수입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "GITHUB_AUTHENTICATION_FAILED", "GitHub 인증에 실패했습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "GITHUB_ACCESS_DENIED", "GitHub 저장소에 접근할 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "GITHUB_RESOURCE_NOT_FOUND", "GitHub 리소스를 찾을 수 없습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "GITHUB_RATE_LIMIT_EXCEEDED", "GitHub API 호출 한도를 초과했습니다."),
    API_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "GITHUB_API_UNAVAILABLE", "GitHub API를 일시적으로 사용할 수 없습니다."),
    CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "GITHUB_CONNECTION_FAILED", "GitHub API에 연결할 수 없습니다."),
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "GITHUB_TIMEOUT", "GitHub API 응답 시간이 초과되었습니다."),
    INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "GITHUB_INVALID_RESPONSE", "GitHub API 응답을 처리할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    GitHubErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
