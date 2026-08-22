package com.orderflow.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Root health-check endpoint.
 * Railway (and most PaaS platforms) probe GET / to determine if the service
 * is alive before routing traffic to it. Without this, every health check
 * hits the generic exception handler and logs a spurious 500.
 */
@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "order-service"));
    }
}
