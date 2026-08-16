package com.orderflow.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderflow.notification.dto.OrderConfirmedEvent;
import com.orderflow.notification.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order events from Kafka and delegates email sending to EmailService.
 *
 * This service never calls order-service directly — it only knows about the
 * event shape on the topic. Decoupling is the entire point: if this service
 * is down, Kafka holds the messages and they're processed when it comes back.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public OrderEventListener(EmailService emailService) {
        this.emailService = emailService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void handleOrderEvent(String message) {
        OrderConfirmedEvent event = null;
        try {
            event = objectMapper.readValue(message, OrderConfirmedEvent.class);
            log.info("KAFKA_EVENT_RECEIVED eventType={} orderId={} userId={}",
                    event.getEventType(), event.getOrderId(), event.getUserId());
        } catch (Exception e) {
            log.error("KAFKA_EVENT_PARSE_FAILED message={} error={}", message, e.getMessage(), e);
            return;
        }

        try {
            if ("CANCELLED".equalsIgnoreCase(event.getEventType())) {
                log.info("NOTIFICATION_CANCELLED orderId={}", event.getOrderId());
                emailService.sendOrderCancelled(event);
            } else {
                // Default: treat any event without explicit type as CONFIRMED
                log.info("NOTIFICATION_CONFIRMED orderId={}", event.getOrderId());
                emailService.sendOrderConfirmed(event);
            }
        } catch (Exception e) {
            // Log but don't rethrow — we don't want a failed notification
            // to cause Kafka to retry and potentially spam the user.
            log.error("NOTIFICATION_SEND_FAILED orderId={} error={}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}
