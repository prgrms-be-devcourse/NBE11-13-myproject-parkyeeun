package com.repoary.backend.analysis.exception;

import com.repoary.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AnalysisErrorCode implements ErrorCode {

    DATE_REQUIRED(HttpStatus.BAD_REQUEST, "ANALYSIS_DATE_REQUIRED", "분석 날짜는 필수입니다."),
    DATE_RANGE_REQUIRED(HttpStatus.BAD_REQUEST, "ANALYSIS_DATE_RANGE_REQUIRED", "분석 시작일과 종료일은 필수입니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "ANALYSIS_INVALID_DATE_RANGE", "분석 시작일은 종료일보다 늦을 수 없습니다."),
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_JOB_NOT_FOUND", "분석 작업을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    AnalysisErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
