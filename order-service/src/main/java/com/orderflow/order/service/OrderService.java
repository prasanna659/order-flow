package com.orderflow.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.client.InventoryClient;
import com.orderflow.order.client.PaymentClient;
import com.orderflow.order.dto.ChargeResult;
import com.orderflow.order.dto.OrderItemDto;
import com.orderflow.order.dto.PlaceOrderRequest;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.exception.*;
import com.orderflow.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final OrderEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderService(OrderRepository orderRepository, InventoryClient inventoryClient,
                        PaymentClient paymentClient, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * STEP 1 of the saga — synchronous.
     * Creates the order as PENDING, returns immediately with the order ID.
     * The client polls GET /api/orders/{id} to watch status transitions.
     */
    public Order placeOrder(PlaceOrderRequest request) {
        BigDecimal total = request.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(request.getUserId());
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        try {
            order.setItemsJson(objectMapper.writeValueAsString(request.getItems()));
        } catch (Exception ignored) {}

        order = orderRepository.save(order);

        // Capture MDC values NOW, on the request thread, before handing off
        // to the async executor (MDC is thread-local and won't transfer automatically).
        String capturedRequestId = MDC.get("requestId");
        String capturedTraceId   = MDC.get("traceId");

        runSaga(order.getId(), request.getItems(), request.getUserEmail(),
                request.getUsername(), capturedRequestId, capturedTraceId);

        return order;
    }

    /**
     * STEPS 2–6 of the saga — asynchronous.
     *
     * This is the orchestrator. It runs on a background thread so the HTTP
     * response in placeOrder() is not blocked.
     *
     * The compensating transaction is in the payment-failed branch: if
     * charge() fails AFTER reserveStock() already committed, we call
     * releaseStock() as a second, explicit write that undoes the first.
     * This is the Saga pattern — no distributed transaction, just
     * explicit compensation.
     */
    @Async
    public void runSaga(String orderId, List<OrderItemDto> items,
                        String userEmail, String username,
                        String requestId, String traceId) {
        // Restore MDC on the async thread for correlated log tracing
        if (requestId != null) MDC.put("requestId", requestId);
        if (traceId   != null) MDC.put("traceId",   traceId);

        try {
            _runSaga(orderId, items, userEmail, username, requestId, traceId);
        } finally {
            MDC.remove("requestId");
            MDC.remove("traceId");
        }
    }

    private void _runSaga(String orderId, List<OrderItemDto> items,
                          String userEmail, String username,
                          String requestId, String traceId) {
        log.info("SAGA_START orderId={} requestId={} traceId={}", orderId, requestId, traceId);
        updateStatus(orderId, OrderStatus.RESERVING, null);

        boolean stockReserved = false;
        try {
            log.info("SAGA_RESERVING_STOCK orderId={}", orderId);
            inventoryClient.reserveStock(orderId, items);
            stockReserved = true;
            log.info("SAGA_STOCK_RESERVED orderId={}", orderId);

            updateStatus(orderId, OrderStatus.CHARGING, null);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order not found: " + orderId));

            log.info("SAGA_CHARGING_PAYMENT orderId={} amount={}", orderId, order.getTotalAmount());
            ChargeResult chargeResult = paymentClient.charge(orderId, order.getTotalAmount());

            if (!chargeResult.isSuccess()) {
                // Payment failed after stock was already reserved — compensate.
                log.warn("SAGA_PAYMENT_FAILED orderId={} reason={}", orderId, chargeResult.getMessage());
                log.info("SAGA_COMPENSATION_START orderId={}", orderId);
                try {
                    inventoryClient.releaseStock(orderId);
                    log.info("SAGA_COMPENSATION_SUCCESS orderId={}", orderId);
                } catch (Exception releaseFailure) {
                    log.error("SAGA_COMPENSATION_FAILED orderId={} error={}", orderId, releaseFailure.getMessage(), releaseFailure);
                    throw new BusinessException("COMPENSATION_FAILED",
                            "Inventory rollback failed for order: " + orderId, releaseFailure);
                }
                updateStatus(orderId, OrderStatus.CANCELLED, chargeResult.getMessage());
                log.info("SAGA_CANCELLED orderId={}", orderId);

                // Notify user of cancellation via Kafka → notification-service
                order = orderRepository.findById(orderId).orElse(order);
                eventPublisher.publishOrderCancelled(order, userEmail, username);
                return;
            }

            // Both steps succeeded — confirm.
            log.info("SAGA_PAYMENT_SUCCESS orderId={}", orderId);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
            log.info("SAGA_ORDER_CONFIRMED orderId={}", orderId);

            // Fire-and-forget: failure here must NOT roll back a confirmed order.
            log.info("SAGA_PUBLISHING_EVENT orderId={}", orderId);
            eventPublisher.publishOrderConfirmed(order, userEmail, username, items);
            log.info("SAGA_SUCCESS orderId={}", orderId);

        } catch (Exception e) {
            log.error("SAGA_FAILURE orderId={} error={}", orderId, e.getMessage(), e);
            if (stockReserved) {
                log.info("SAGA_DEFENSIVE_COMPENSATION_START orderId={}", orderId);
                try {
                    inventoryClient.releaseStock(orderId);
                    log.info("SAGA_DEFENSIVE_COMPENSATION_SUCCESS orderId={}", orderId);
                } catch (Exception releaseFailure) {
                    log.error("SAGA_DEFENSIVE_COMPENSATION_FAILED orderId={} error={}",
                            orderId, releaseFailure.getMessage(), releaseFailure);
                }
            }
            updateStatus(orderId, OrderStatus.CANCELLED, e.getMessage());
            log.info("SAGA_CANCELLED orderId={}", orderId);

            // Publish cancellation event so the notification-service can email the user.
            // This path covers unexpected failures (inventory timeout, circuit breaker, etc.)
            // that bypass the explicit chargeResult.isSuccess() == false branch above.
            try {
                Order cancelledOrder = orderRepository.findById(orderId).orElse(null);
                if (cancelledOrder != null) {
                    eventPublisher.publishOrderCancelled(cancelledOrder, userEmail, username);
                }
            } catch (Exception publishEx) {
                log.error("SAGA_CANCEL_EVENT_PUBLISH_FAILED orderId={} error={}", orderId, publishEx.getMessage(), publishEx);
            }
        }
    }

    private void updateStatus(String orderId, OrderStatus status, String failureReason) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            order.setFailureReason(failureReason);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
        });
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order not found: " + orderId));
    }

    public List<Order> getOrdersForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public org.springframework.data.domain.Page<Order> getOrdersForUserPaginated(
            Long userId, OrderStatus status, int page, int size) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        }
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<OrderItemDto> getOrderItems(Order order) {
        try {
            if (order.getItemsJson() != null && !order.getItemsJson().isEmpty()) {
                return objectMapper.readValue(order.getItemsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, OrderItemDto.class));
            }
        } catch (Exception e) {
            log.error("Failed to parse order items for order {}: {}", order.getId(), e.getMessage());
        }
        return List.of();
    }
}
