package com.repoary.backend.repository.exception;

import com.repoary.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum RepositoryErrorCode implements ErrorCode {

    CONNECTED_REPOSITORY_NOT_FOUND(HttpStatus.NOT_FOUND, "REPOSITORY_NOT_FOUND", "연결된 저장소를 찾을 수 없습니다."),
    INVALID_CONNECT_REQUEST(HttpStatus.BAD_REQUEST, "REPOSITORY_INVALID_CONNECT_REQUEST", "저장소 연결 요청이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    RepositoryErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
