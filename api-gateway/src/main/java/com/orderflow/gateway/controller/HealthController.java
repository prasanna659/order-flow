package com.orderflow.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Root health-check endpoint for the API Gateway.
 *
 * Railway probes GET / before routing traffic to the service.
 * Without this, every health check produces a 404 "No static resource ."
 * logged as an error by the GatewayExceptionHandler.
 *
 * Spring Cloud Gateway runs on WebFlux, so the return type is Mono<>.
 */
@RestController
public class HealthController {

    @GetMapping("/")
    public Mono<ResponseEntity<Map<String, String>>> health() {
        return Mono.just(ResponseEntity.ok(Map.of("status", "UP", "service", "api-gateway")));
    }
}
