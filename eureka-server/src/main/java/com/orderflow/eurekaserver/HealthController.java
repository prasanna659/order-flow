package com.orderflow.eurekaserver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Root health-check endpoint.
 * Railway probes GET / before routing traffic to the service.
 * Note: Eureka Server already serves its dashboard at /, but this
 * ensures a clean JSON response for programmatic health checks.
 */
@RestController
public class HealthController {

    @GetMapping("/health-check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "eureka-server"));
    }
}
