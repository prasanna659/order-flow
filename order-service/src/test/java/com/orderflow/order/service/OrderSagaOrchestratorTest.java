package com.orderflow.order.service;

import com.orderflow.order.client.InventoryClient;
import com.orderflow.order.client.PaymentClient;
import com.orderflow.order.dto.ChargeResult;
import com.orderflow.order.dto.OrderItemDto;
import com.orderflow.order.entity.Order;
import com.orderflow.order.entity.OrderStatus;
import com.orderflow.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the order saga. The saga body is called synchronously via
 * {@code execute(...)}, so no Spring context, database or Kafka is needed.
 */
@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    private static final String ORDER_ID = "order-1";

    @Mock private OrderRepository orderRepository;
    @Mock private InventoryClient inventoryClient;
    @Mock private PaymentClient paymentClient;
    @Mock private OrderEventPublisher eventPublisher;

    private OrderSagaOrchestrator saga;
    private Order order;
    private List<OrderItemDto> items;

    @BeforeEach
    void setUp() {
        saga = new OrderSagaOrchestrator(orderRepository, inventoryClient, paymentClient, eventPublisher);

        order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(42L);
        order.setTotalAmount(new BigDecimal("199.98"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        OrderItemDto item = new OrderItemDto();
        item.setProductId(1L);
        item.setProductName("Keyboard");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("99.99"));
        items = List.of(item);

        // The repository hands back the same Order instance, so status changes are visible to assertions.
        lenient().when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void runSaga() {
        saga.execute(ORDER_ID, items, "user@example.com", "user", "req-1", "trace-1");
    }

    @Test
    @DisplayName("happy path: stock reserved, payment approved -> CONFIRMED and confirmation event published")
    void happyPath() {
        when(paymentClient.charge(ORDER_ID, order.getTotalAmount()))
                .thenReturn(new ChargeResult(true, "tx-1", "Payment approved"));

        runSaga();

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(inventoryClient).reserveStock(ORDER_ID, items);
        verify(inventoryClient, never()).releaseStock(anyString());
        verify(eventPublisher).publishOrderConfirmed(order, "user@example.com", "user", items);
        verify(eventPublisher, never()).publishOrderCancelled(any(), any(), any());
    }

    @Test
    @DisplayName("payment declined: stock is released (compensation) and the order is CANCELLED")
    void paymentDeclinedTriggersCompensation() {
        when(paymentClient.charge(ORDER_ID, order.getTotalAmount()))
                .thenReturn(new ChargeResult(false, "tx-2", "Payment declined by issuing bank"));

        runSaga();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Payment declined by issuing bank", order.getFailureReason());
        verify(inventoryClient).reserveStock(ORDER_ID, items);
        verify(inventoryClient, times(1)).releaseStock(ORDER_ID);
        verify(eventPublisher).publishOrderCancelled(order, "user@example.com", "user");
        verify(eventPublisher, never()).publishOrderConfirmed(any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("stock reservation fails: payment is never attempted, nothing to release, order CANCELLED")
    void reservationFailureSkipsPaymentAndRelease() {
        doThrow(new InventoryClient.StockReservationException("Insufficient stock"))
                .when(inventoryClient).reserveStock(eq(ORDER_ID), anyList());

        runSaga();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertNotNull(order.getFailureReason());
        assertTrue(order.getFailureReason().contains("Insufficient stock"));
        verify(paymentClient, never()).charge(anyString(), any());
        verify(inventoryClient, never()).releaseStock(anyString());
        verify(eventPublisher).publishOrderCancelled(order, "user@example.com", "user");
    }

    @Test
    @DisplayName("payment service blows up after stock was reserved: defensive release, order CANCELLED")
    void paymentExceptionTriggersDefensiveCompensation() {
        when(paymentClient.charge(ORDER_ID, order.getTotalAmount()))
                .thenThrow(new RuntimeException("connection reset"));

        runSaga();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(inventoryClient, times(1)).releaseStock(ORDER_ID);
        verify(eventPublisher).publishOrderCancelled(order, "user@example.com", "user");
        verify(eventPublisher, never()).publishOrderConfirmed(any(), any(), any(), anyList());
    }

    @Test
    @DisplayName("compensation itself fails: order still ends CANCELLED with the rollback failure recorded")
    void compensationFailureStillCancelsOrder() {
        when(paymentClient.charge(ORDER_ID, order.getTotalAmount()))
                .thenReturn(new ChargeResult(false, "tx-3", "Payment declined by issuing bank"));
        doThrow(new RuntimeException("inventory down")).when(inventoryClient).releaseStock(ORDER_ID);

        runSaga();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertNotNull(order.getFailureReason());
        assertTrue(order.getFailureReason().contains("Inventory rollback failed"));
        verify(eventPublisher).publishOrderCancelled(order, "user@example.com", "user");
    }

    @Test
    @DisplayName("status moves PENDING -> RESERVING -> CHARGING -> CONFIRMED in order")
    void statusTransitionsInOrder() {
        List<OrderStatus> seen = new java.util.ArrayList<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order saved = inv.getArgument(0);
            seen.add(saved.getStatus());
            return saved;
        });
        when(paymentClient.charge(ORDER_ID, order.getTotalAmount()))
                .thenReturn(new ChargeResult(true, "tx-4", "Payment approved"));

        runSaga();

        assertEquals(List.of(OrderStatus.RESERVING, OrderStatus.CHARGING, OrderStatus.CONFIRMED), seen);
    }
}
