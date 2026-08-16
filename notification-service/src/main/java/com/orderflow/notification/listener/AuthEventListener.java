package com.orderflow.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orderflow.notification.dto.PasswordResetEvent;
import com.orderflow.notification.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes auth-events from Kafka (password reset, future auth events).
 * Fully decoupled from auth-service — only knows about the event shape.
 */
@Component
public class AuthEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuthEventListener.class);

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public AuthEventListener(EmailService emailService) {
        this.emailService = emailService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "auth-events", groupId = "notification-service")
    public void handleAuthEvent(String message) {
        PasswordResetEvent event;
        try {
            event = objectMapper.readValue(message, PasswordResetEvent.class);
        } catch (Exception e) {
            // JSON could not be deserialized — log and discard, nothing to retry
            log.error("AUTH_EVENT_PARSE_FAILED error={} message={}", e.getMessage(), message, e);
            return;
        }

        log.info("AUTH_EVENT_RECEIVED eventType={} userId={}", event.getEventType(), event.getUserId());

        // sendPasswordResetEmail / sendOrderXxx are annotated @Retryable.
        // Any exception that escapes here means all retry attempts were exhausted.
        // Catch it separately from the parse block so the log message is accurate.
        try {
            if ("PASSWORD_RESET".equalsIgnoreCase(event.getEventType())) {
                emailService.sendPasswordResetEmail(event);
            } else {
                log.warn("AUTH_EVENT_UNKNOWN_TYPE eventType={}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("AUTH_EVENT_SEND_FAILED eventType={} userId={} error={}",
                    event.getEventType(), event.getUserId(), e.getMessage(), e);
        }
    }
}
