package com.orderflow.order.exception;

public class OrderCancelledException extends RuntimeException {
    private final String errorCode;

    public OrderCancelledException(String message) {
        super(message);
        this.errorCode = "ORDER_CANCELLED";
    }

    public OrderCancelledException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
