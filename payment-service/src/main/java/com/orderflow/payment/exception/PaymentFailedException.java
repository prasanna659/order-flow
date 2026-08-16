package com.orderflow.payment.exception;

public class PaymentFailedException extends RuntimeException {
    private final String errorCode;

    public PaymentFailedException(String message) {
        super(message);
        this.errorCode = "PAYMENT_FAILED";
    }

    public PaymentFailedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
