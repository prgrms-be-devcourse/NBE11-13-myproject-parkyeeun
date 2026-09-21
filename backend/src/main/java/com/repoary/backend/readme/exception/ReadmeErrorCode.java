package com.repoary.backend.readme.exception;

import com.repoary.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ReadmeErrorCode implements ErrorCode {

    DATE_REQUIRED(HttpStatus.BAD_REQUEST, "README_DATE_REQUIRED", "README 날짜는 필수입니다."),
    INVALID_TIL_FORMAT(HttpStatus.BAD_REQUEST, "README_INVALID_TIL_FORMAT", "README 행을 생성할 수 있는 TIL 형식이 아닙니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ReadmeErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
