package com.orderflow.inventory.dto;

import java.util.List;

public class ReserveRequest {
    private String orderId;
    private List<ReserveItem> items;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public List<ReserveItem> getItems() { return items; }
    public void setItems(List<ReserveItem> items) { this.items = items; }
}
