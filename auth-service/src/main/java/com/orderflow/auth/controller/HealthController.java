package com.orderflow.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Root health-check endpoint.
 * Railway probes GET / before routing traffic to the service.
 * Without this, every health check hits the generic exception handler
 * and logs a spurious 500 with INTERNAL_ERROR.
 */
@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth-service"));
    }
}
