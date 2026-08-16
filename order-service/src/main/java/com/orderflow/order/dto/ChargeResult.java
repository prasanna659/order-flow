package com.orderflow.order.dto;

public class ChargeResult {
    private boolean success;
    private String transactionId;
    private String message;

    public ChargeResult(boolean success, String transactionId, String message) {
        this.success = success;
        this.transactionId = transactionId;
        this.message = message;
    }
    public boolean isSuccess() { return success; }
    public String getTransactionId() { return transactionId; }
    public String getMessage() { return message; }
}
