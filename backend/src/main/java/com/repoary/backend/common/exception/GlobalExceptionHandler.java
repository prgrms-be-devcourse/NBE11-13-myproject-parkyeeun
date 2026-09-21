package com.repoary.backend.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(
            GlobalExceptionHandler.class
    );

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception
    ) {
        return errorResponse(exception.getErrorCode());
    }

    @ExceptionHandler(ExternalSystemException.class)
    public ResponseEntity<ErrorResponse> handleExternalSystemException(
            ExternalSystemException exception
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn(
                "External system request failed. code={}, causeType={}",
                errorCode.getCode(),
                exception.getCause() == null
                        ? "unknown"
                        : exception.getCause().getClass().getName()
        );
        return errorResponse(errorCode);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(
            Exception exception
    ) {
        log.debug(
                "Invalid request. exceptionType={}",
                exception.getClass().getName()
        );
        return errorResponse(CommonErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequestBody() {
        log.debug("Request body could not be read.");
        return errorResponse(CommonErrorCode.INVALID_REQUEST_BODY);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            BindException.class
    })
    public ResponseEntity<ErrorResponse> handleValidationException(
            Exception exception
    ) {
        log.debug(
                "Request validation failed. exceptionType={}",
                exception.getClass().getName()
        );
        return errorResponse(CommonErrorCode.VALIDATION_FAILED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception
    ) {
        log.error(
                "Unexpected server error. exceptionType={}, location={}, causeType={}",
                exception.getClass().getName(),
                firstStackFrame(exception),
                exception.getCause() == null
                        ? "none"
                        : exception.getCause().getClass().getName()
        );
        return errorResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    private String firstStackFrame(Exception exception) {
        StackTraceElement[] stackTrace = exception.getStackTrace();
        return stackTrace.length == 0
                ? "unknown"
                : stackTrace[0].toString();
    }

    private ResponseEntity<ErrorResponse> errorResponse(
            ErrorCode errorCode
    ) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }
}
