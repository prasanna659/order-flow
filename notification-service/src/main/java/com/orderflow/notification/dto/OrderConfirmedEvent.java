package com.orderflow.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Mirrors the event shape published by order-service.
 * @JsonIgnoreProperties(ignoreUnknown = true) ensures extra fields from
 * order-service (e.g. productId on items) never break deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderConfirmedEvent {
    private String orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private String userEmail;
    private String username;
    private String failureReason;
    private String eventType;
    private Instant timestamp;
    private List<OrderItemLine> items;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemLine {
        private String productName;
        private Integer quantity;
        private BigDecimal price;

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public List<OrderItemLine> getItems() { return items; }
    public void setItems(List<OrderItemLine> items) { this.items = items; }
}
