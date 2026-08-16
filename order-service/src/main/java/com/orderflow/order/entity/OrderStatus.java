package com.orderflow.order.entity;

public enum OrderStatus {
    PENDING,        // just created, saga hasn't started or is mid-flight
    RESERVING,      // calling inventory-service
    CHARGING,       // calling payment-service
    CONFIRMED,      // saga completed successfully
    CANCELLED       // saga failed at some step, compensations applied
}
