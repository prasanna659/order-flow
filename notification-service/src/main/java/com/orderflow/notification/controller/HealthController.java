package com.orderflow.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Root health-check endpoint.
 * Railway probes GET / before routing traffic to the service.
 * Without this, every health check returns the Whitelabel Error Page
 * because this service has no HTTP controllers (it is Kafka-driven only).
 */
@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "notification-service"));
    }
}
