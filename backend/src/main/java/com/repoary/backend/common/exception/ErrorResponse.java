package com.repoary.backend.common.exception;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ErrorResponse(
        int status,
        String code,
        String message,
        OffsetDateTime timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
