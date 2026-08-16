package com.orderflow.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // Plain WebClient.Builder — service URLs are injected via environment
    // variables (INVENTORY_SERVICE_URL, PAYMENT_SERVICE_URL) so we don't
    // need Eureka / @LoadBalanced for Railway deployments.
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
