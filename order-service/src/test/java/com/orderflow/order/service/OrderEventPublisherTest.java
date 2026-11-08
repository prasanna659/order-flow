package com.orderflow.order.service;

import com.orderflow.order.dto.OrderItemDto;
import com.orderflow.order.entity.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private Order order() {
        Order o = new Order();
        o.setId("order-9");
        o.setUserId(7L);
        o.setTotalAmount(new BigDecimal("50.00"));
        o.setFailureReason("Payment declined by issuing bank");
        return o;
    }

    @Test
    @DisplayName("confirmed event goes to order-events, keyed by order id, with the contact details")
    void publishesConfirmedEvent() {
        OrderEventPublisher publisher = new OrderEventPublisher(kafkaTemplate);
        OrderItemDto item = new OrderItemDto();
        item.setProductId(1L);
        item.setQuantity(1);
        item.setPrice(new BigDecimal("50.00"));

        publisher.publishOrderConfirmed(order(), "a@b.com", "alice", List.of(item));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("order-events"),
                org.mockito.ArgumentMatchers.eq("order-9"), payload.capture());
        assertTrue(payload.getValue().contains("\"eventType\":\"CONFIRMED\""));
        assertTrue(payload.getValue().contains("\"userEmail\":\"a@b.com\""));
        assertTrue(payload.getValue().contains("\"orderId\":\"order-9\""));
    }

    @Test
    @DisplayName("cancelled event carries the failure reason")
    void publishesCancelledEventWithReason() {
        OrderEventPublisher publisher = new OrderEventPublisher(kafkaTemplate);

        publisher.publishOrderCancelled(order(), "a@b.com", "alice");

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("order-events"),
                org.mockito.ArgumentMatchers.eq("order-9"), payload.capture());
        assertTrue(payload.getValue().contains("\"eventType\":\"CANCELLED\""));
        assertTrue(payload.getValue().contains("Payment declined by issuing bank"));
    }

    @Test
    @DisplayName("a Kafka failure never propagates (a confirmed order must not be undone by a failed email event)")
    void kafkaFailureIsSwallowed() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("broker unavailable"));
        OrderEventPublisher publisher = new OrderEventPublisher(kafkaTemplate);

        assertDoesNotThrow(() -> publisher.publishOrderConfirmed(order(), "a@b.com", "alice", List.of()));
        assertDoesNotThrow(() -> publisher.publishOrderCancelled(order(), "a@b.com", "alice"));
    }
}
