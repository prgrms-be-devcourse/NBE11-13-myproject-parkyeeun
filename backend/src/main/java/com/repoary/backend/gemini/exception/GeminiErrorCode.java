package com.repoary.backend.gemini.exception;

import com.repoary.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GeminiErrorCode implements ErrorCode {

    API_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "GEMINI_API_KEY_REQUIRED", "Gemini API Key가 필요합니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "GEMINI_AUTHENTICATION_FAILED", "Gemini 인증에 실패했습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "GEMINI_RATE_LIMIT_EXCEEDED", "Gemini API 호출 한도를 초과했습니다."),
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "GEMINI_TIMEOUT", "Gemini API 응답 시간이 초과되었습니다."),
    CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "GEMINI_CONNECTION_FAILED", "Gemini API에 연결할 수 없습니다."),
    API_ERROR(HttpStatus.BAD_GATEWAY, "GEMINI_API_ERROR", "Gemini API 요청에 실패했습니다."),
    INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "GEMINI_INVALID_RESPONSE", "Gemini API 응답을 처리할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    GeminiErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
