package com.orderflow.auth.exception;

public class InvalidTokenException extends RuntimeException {
    private final String errorCode;

    public InvalidTokenException(String message) {
        super(message);
        this.errorCode = "INVALID_TOKEN";
    }

    public InvalidTokenException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
