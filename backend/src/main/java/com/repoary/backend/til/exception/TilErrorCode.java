package com.repoary.backend.til.exception;

import com.repoary.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum TilErrorCode implements ErrorCode {

    DATE_REQUIRED(HttpStatus.BAD_REQUEST, "TIL_DATE_REQUIRED", "TIL 날짜는 필수입니다."),
    ALREADY_EXISTS(HttpStatus.CONFLICT, "TIL_ALREADY_EXISTS", "해당 날짜의 TIL이 이미 존재합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "TIL_NOT_FOUND", "TIL을 찾을 수 없습니다."),
    DATE_NOT_FOUND(HttpStatus.NOT_FOUND, "TIL_DATE_NOT_FOUND", "해당 날짜의 TIL을 찾을 수 없습니다."),
    CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "TIL_CONTENT_REQUIRED", "TIL 내용은 필수입니다."),
    COMPLETED_ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "TIL_COMPLETED_ANALYSIS_NOT_FOUND", "완료된 분석 결과를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    TilErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
