package com.orderflow.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .description("Saga orchestrator. POST /api/orders starts the distributed transaction: " +
                                "reserve stock → charge payment → confirm. Returns 202 immediately; " +
                                "poll GET /api/orders/{id} to watch the saga progress.")
                        .version("1.0.0")
                        .contact(new Contact().name("OrderFlow").url("http://localhost:3000")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Via API Gateway"),
                        new Server().url("http://localhost:8083").description("Direct (dev)")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token from /api/auth/login")));
    }
}
