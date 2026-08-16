package com.orderflow.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    private final String errorCode;

    public InsufficientStockException(String message) {
        super(message);
        this.errorCode = "INSUFFICIENT_STOCK";
    }

    public InsufficientStockException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
