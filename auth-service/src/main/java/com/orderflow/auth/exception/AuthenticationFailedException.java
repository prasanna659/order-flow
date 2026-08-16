package com.orderflow.auth.exception;

public class AuthenticationFailedException extends RuntimeException {
    private final String errorCode;

    public AuthenticationFailedException(String message) {
        super(message);
        this.errorCode = "AUTHENTICATION_FAILED";
    }

    public AuthenticationFailedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
