package com.orderflow.order.service;

import com.orderflow.order.client.InventoryClient;
import com.orderflow.order.client.PaymentClient;
import com.orderflow.order.dto.ChargeResult;
import com.orderflow.order.dto.OrderItemDto;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.exception.BusinessException;
import com.orderflow.order.exception.ResourceNotFoundException;
import com.orderflow.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Runs the order saga: reserve stock -> charge payment -> confirm.
 *
 * This lives in its own bean on purpose. {@code @Async} only works when the
 * call goes through the Spring proxy. When the saga was a method on
 * OrderService and was called from placeOrder() in the same class, the call
 * bypassed the proxy and ran synchronously on the HTTP request thread.
 * Calling it from a different bean fixes that.
 */
@Component
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final OrderEventPublisher eventPublisher;

    public OrderSagaOrchestrator(OrderRepository orderRepository,
                                 InventoryClient inventoryClient,
                                 PaymentClient paymentClient,
                                 OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Entry point. Runs on the "taskExecutor" pool so the HTTP response from
     * placeOrder() is not blocked by the downstream calls.
     */
    @Async("taskExecutor")
    public void runSaga(String orderId, List<OrderItemDto> items,
                        String userEmail, String username,
                        String requestId, String traceId) {
        // MDC is thread-local, so restore it on the async thread for log correlation.
        if (requestId != null) MDC.put("requestId", requestId);
        if (traceId != null) MDC.put("traceId", traceId);
        try {
            execute(orderId, items, userEmail, username, requestId, traceId);
        } finally {
            MDC.remove("requestId");
            MDC.remove("traceId");
        }
    }

    /** The saga body. Package-private so tests can call it directly, without async. */
    void execute(String orderId, List<OrderItemDto> items,
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
                // Payment failed after stock was reserved: run the compensating action.
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

                order = orderRepository.findById(orderId).orElse(order);
                eventPublisher.publishOrderCancelled(order, userEmail, username);
                return;
            }

            // Both steps succeeded: confirm.
            log.info("SAGA_PAYMENT_SUCCESS orderId={}", orderId);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
            log.info("SAGA_ORDER_CONFIRMED orderId={}", orderId);

            // A failed notification must never undo a confirmed order.
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
}
