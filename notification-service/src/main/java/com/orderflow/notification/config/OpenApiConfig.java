package com.orderflow.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
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
                        .title("Notification Service API")
                        .description("Kafka consumer that sends transactional emails on order events. " +
                                "Fully decoupled — never called directly by any other service. " +
                                "Set MAIL_USERNAME + MAIL_PASSWORD env vars to enable real Gmail SMTP delivery.")
                        .version("1.0.0")
                        .contact(new Contact().name("OrderFlow").url("http://localhost:3000")))
                .servers(List.of(
                        new Server().url("http://localhost:8085").description("Direct (dev)")));
    }
}
