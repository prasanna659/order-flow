package com.orderflow.inventory.service;

@Deprecated
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
