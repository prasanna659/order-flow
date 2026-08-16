package com.orderflow.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderflow.order.entity.Order;
import com.orderflow.order.dto.OrderItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Publishes order lifecycle events to Kafka.
 *
 * The event payload is enriched with userEmail and username so the
 * notification-service can send real emails without needing to call
 * any other service (fully decoupled).
 *
 * Failure here is deliberately non-fatal for CONFIRMED orders — the
 * order is already paid and the stock is committed. A failed notification
 * publish should never undo a real financial transaction.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Publish an order.confirmed event after successful saga completion.
     * Enriches the payload with user contact info for email delivery.
     */
    public void publishOrderConfirmed(Order order, String userEmail, String username, List<OrderItemDto> items) {
        try {
            Map<String, Object> event = buildEventPayload(order, userEmail, username, items, "CONFIRMED");
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("order-events", order.getId(), json);
            log.info("KAFKA_PUBLISHED eventType=CONFIRMED orderId={}", order.getId());
        } catch (Exception e) {
            // Non-fatal — order is already confirmed in the database.
            log.error("KAFKA_PUBLISH_FAILED eventType=CONFIRMED orderId={} error={}", order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publish an order.cancelled event after saga compensation.
     * Includes the failure reason so the email template can display it.
     */
    public void publishOrderCancelled(Order order, String userEmail, String username) {
        try {
            Map<String, Object> event = buildEventPayload(order, userEmail, username, null, "CANCELLED");
            event.put("failureReason", order.getFailureReason());
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("order-events", order.getId(), json);
            log.info("KAFKA_PUBLISHED eventType=CANCELLED orderId={}", order.getId());
        } catch (Exception e) {
            log.error("KAFKA_PUBLISH_FAILED eventType=CANCELLED orderId={} error={}", order.getId(), e.getMessage(), e);
        }
    }

    private Map<String, Object> buildEventPayload(Order order, String userEmail, String username,
                                                    List<OrderItemDto> items, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("orderId", order.getId());
        event.put("userId", order.getUserId());
        event.put("totalAmount", order.getTotalAmount());
        event.put("userEmail", userEmail != null ? userEmail : "");
        event.put("username", username != null ? username : "User");
        event.put("eventType", eventType);
        event.put("timestamp", Instant.now().toString());
        if (items != null) {
            event.put("items", items);
        }
        return event;
    }
}
