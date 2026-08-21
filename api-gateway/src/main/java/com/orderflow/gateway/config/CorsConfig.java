package com.orderflow.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Centralized CORS configuration for the API Gateway.
 * This is the single source of truth for CORS across the entire application.
 * Individual services should NOT configure CORS to avoid duplicate headers.
 */
@Configuration
public class CorsConfig {

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Allow the deployed frontend origin plus localhost for local dev.
        // Origins must NOT have a trailing slash — exact match is required.
        corsConfig.setAllowedOrigins(List.of(
            frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl,
            "http://localhost:3000",
            "http://localhost:5173"
        ));
        
        // Allow credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(true);
        
        // Allow common HTTP methods
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allow all headers
        corsConfig.setAllowedHeaders(List.of("*"));
        
        // Cache preflight requests for 1 hour
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return new CorsWebFilter(source);
    }
}
