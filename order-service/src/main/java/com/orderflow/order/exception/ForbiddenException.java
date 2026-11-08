package com.orderflow.order.exception;

/** Thrown when the caller tries to access another user's data. Mapped to HTTP 403. */
public class ForbiddenException extends RuntimeException {
    private final String errorCode;

    public ForbiddenException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
