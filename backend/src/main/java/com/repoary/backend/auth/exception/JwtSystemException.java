package com.repoary.backend.auth.exception;

public class JwtSystemException extends RuntimeException {

    public JwtSystemException(Throwable cause) {
        super("JWT 처리 중 내부 오류가 발생했습니다.", cause);
    }
}
