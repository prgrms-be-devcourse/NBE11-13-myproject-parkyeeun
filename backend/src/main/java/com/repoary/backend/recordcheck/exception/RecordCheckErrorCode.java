package com.repoary.backend.recordcheck.exception;

import com.repoary.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum RecordCheckErrorCode implements ErrorCode {

    MONTH_REQUIRED(HttpStatus.BAD_REQUEST, "RECORD_CHECK_MONTH_REQUIRED", "점검할 월은 필수입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    RecordCheckErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
