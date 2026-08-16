package com.orderflow.order.exception;

public class OrderAlreadyConfirmedException extends RuntimeException {
    private final String errorCode;

    public OrderAlreadyConfirmedException(String message) {
        super(message);
        this.errorCode = "ORDER_ALREADY_CONFIRMED";
    }

    public OrderAlreadyConfirmedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
