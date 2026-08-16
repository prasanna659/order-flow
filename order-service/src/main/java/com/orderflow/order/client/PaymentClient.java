package com.orderflow.order.client;

import com.orderflow.order.dto.ChargeResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PaymentClient {

    private final WebClient webClient;

    // Base URL injected from application.yml / Railway env var PAYMENT_SERVICE_URL.
    // Falls back to localhost for local dev without Eureka.
    public PaymentClient(WebClient.Builder webClientBuilder,
                         @Value("${services.payment-url:http://localhost:8084}") String paymentServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(paymentServiceUrl).build();
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "chargeFallback")
    public ChargeResult charge(String orderId, BigDecimal amount) {
        Map<?, ?> response = webClient.post()
                .uri("/api/payments/charge")
                .bodyValue(Map.of("orderId", orderId, "amount", amount))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        boolean success = (boolean) response.get("success");
        String transactionId = (String) response.get("transactionId");
        String message = (String) response.get("message");
        return new ChargeResult(success, transactionId, message);
    }

    // If payment-service itself is unreachable (not just "declined"), treat
    // it as a failed charge -- order-service's saga logic then runs the
    // exact same compensating release() it would run for a real decline.
    // One failure path handles both cases; no special-casing needed.
    public ChargeResult chargeFallback(String orderId, BigDecimal amount, Throwable t) {
        return new ChargeResult(false, null, "Payment service unavailable: " + t.getMessage());
    }
}
