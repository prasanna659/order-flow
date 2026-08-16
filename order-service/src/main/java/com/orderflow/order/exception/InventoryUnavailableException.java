package com.orderflow.order.exception;

public class InventoryUnavailableException extends RuntimeException {
    private final String errorCode;

    public InventoryUnavailableException(String message) {
        super(message);
        this.errorCode = "INVENTORY_UNAVAILABLE";
    }

    public InventoryUnavailableException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public InventoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVENTORY_UNAVAILABLE";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
