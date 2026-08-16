package com.orderflow.payment.dto;

import java.math.BigDecimal;

public class ChargeRequest {
    private String orderId;
    private BigDecimal amount;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
