package com.orderflow.payment.dto;

public class ChargeResponse {
    private boolean success;
    private String transactionId;
    private String message;

    public ChargeResponse(boolean success, String transactionId, String message) {
        this.success = success;
        this.transactionId = transactionId;
        this.message = message;
    }
    public boolean isSuccess() { return success; }
    public String getTransactionId() { return transactionId; }
    public String getMessage() { return message; }
}
