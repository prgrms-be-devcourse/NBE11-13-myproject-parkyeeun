package com.repoary.backend.rule.exception;

import com.repoary.backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum RuleErrorCode implements ErrorCode {

    CLASSIFICATION_RULE_NOT_FOUND(HttpStatus.NOT_FOUND, "RULE_CLASSIFICATION_NOT_FOUND", "경로 규칙을 찾을 수 없습니다."),
    CONVENTION_RULE_NOT_FOUND(HttpStatus.NOT_FOUND, "RULE_CONVENTION_NOT_FOUND", "커밋 규칙을 찾을 수 없습니다."),
    PATH_PATTERN_REQUIRED(HttpStatus.BAD_REQUEST, "RULE_PATH_PATTERN_REQUIRED", "경로 패턴은 필수입니다."),
    CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "RULE_CATEGORY_REQUIRED", "카테고리는 필수입니다."),
    MESSAGE_PATTERN_REQUIRED(HttpStatus.BAD_REQUEST, "RULE_MESSAGE_PATTERN_REQUIRED", "커밋 메시지 패턴은 필수입니다."),
    MATCH_CONDITION_REQUIRED(HttpStatus.BAD_REQUEST, "RULE_MATCH_CONDITION_REQUIRED", "commitType, scope, category 중 하나 이상은 필요합니다."),
    INVALID_PRIORITY(HttpStatus.BAD_REQUEST, "RULE_INVALID_PRIORITY", "우선순위는 0 이상이어야 합니다."),
    DUPLICATE_PATH_PATTERN(HttpStatus.BAD_REQUEST, "RULE_DUPLICATE_PATH_PATTERN", "이미 존재하는 경로 패턴입니다."),
    DUPLICATE_MESSAGE_PATTERN(HttpStatus.BAD_REQUEST, "RULE_DUPLICATE_MESSAGE_PATTERN", "이미 존재하는 커밋 메시지 패턴입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    RuleErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
