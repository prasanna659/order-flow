package com.orderflow.order.exception;

public class ServiceUnavailableException extends RuntimeException {
    private final String errorCode;

    public ServiceUnavailableException(String message) {
        super(message);
        this.errorCode = "SERVICE_UNAVAILABLE";
    }

    public ServiceUnavailableException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SERVICE_UNAVAILABLE";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
