package com.orderflow.order.client;

import com.orderflow.order.dto.OrderItemDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class InventoryClient {

    private final WebClient webClient;

    public InventoryClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder.baseUrl("http://inventory-service").build();
    }

    // Circuit breaker: if inventory-service is failing repeatedly (not just
    // "declined this one request" but actually unreachable/erroring), stop
    // hammering it and fail fast via the fallback instead of piling up
    // threads waiting on a dead service. Config lives in application.yml
    // under resilience4j.circuitbreaker.instances.inventoryService.
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveFallback")
    public void reserveStock(String orderId, List<OrderItemDto> items) {
        List<Map<String, Object>> payload = items.stream()
                .map(i -> Map.<String, Object>of("productId", i.getProductId(), "quantity", i.getQuantity()))
                .toList();

        webClient.post()
                .uri("/api/inventory/reserve")
                .bodyValue(Map.of("orderId", orderId, "items", payload))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void reserveFallback(String orderId, List<OrderItemDto> items, Throwable t) {
        // Surface as the same kind of failure the saga already knows how to
        // handle (insufficient stock / 409), so placeOrderSaga() doesn't
        // need a separate code path for "service unreachable" vs "declined".
        throw new StockReservationException(
                "Inventory service unavailable: " + t.getMessage());
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "releaseFallback")
    public void releaseStock(String orderId) {
        webClient.post()
                .uri("/api/inventory/release")
                .bodyValue(Map.of("orderId", orderId))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void releaseFallback(String orderId, Throwable t) {
        // Deliberately swallow this -- release() is itself a compensating
        // action. If it fails, throwing here would mask the ORIGINAL
        // failure (payment decline) that triggered the release in the
        // first place. In production this would instead be written to a
        // dead-letter queue / retried by a reconciliation job so the stock
        // count doesn't drift, but that's beyond this project's scope.
        System.err.println("WARNING: failed to release stock for order " + orderId + ": " + t.getMessage());
    }

    public static class StockReservationException extends RuntimeException {
        public StockReservationException(String message) {
            super(message);
        }
    }
}
