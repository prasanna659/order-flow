package com.orderflow.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // @LoadBalanced means URIs like "http://inventory-service/..." are
    // resolved through Eureka to an actual host:port at call time -- we
    // never hardcode addresses or ports for other services anywhere in
    // this codebase. That's the entire point of the service registry.
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
