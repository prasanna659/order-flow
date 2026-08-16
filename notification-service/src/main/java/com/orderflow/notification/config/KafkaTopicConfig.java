package com.orderflow.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Pre-declares Kafka topics so the notification-service consumer never races
 * against the first publish from auth-service or order-service.
 *
 * Without this, topics are created on first produce (by auth-service /
 * order-service). If notification-service's consumer subscribes *before*
 * the topic exists and Kafka hasn't auto-created it yet, the consumer
 * registers with no starting offset and can silently miss the very first
 * message published after topic creation.
 *
 * KafkaAdmin (auto-configured by Spring Boot when spring-kafka is on the
 * classpath) picks up these @Bean NewTopic definitions and creates them
 * idempotently at startup — safe to call even when topics already exist.
 */
@Configuration
public class KafkaTopicConfig {

    /** Order lifecycle events: CONFIRMED, CANCELLED */
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name("order-events")
                .partitions(1)
                .replicas(1)
                .build();
    }

    /** Auth events: PASSWORD_RESET (future: email-verified, etc.) */
    @Bean
    public NewTopic authEventsTopic() {
        return TopicBuilder.name("auth-events")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
