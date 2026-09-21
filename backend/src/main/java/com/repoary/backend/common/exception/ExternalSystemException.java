package com.repoary.backend.common.exception;

public class ExternalSystemException extends RuntimeException {

    private final ErrorCode errorCode;

    public ExternalSystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
