package com.orderflow.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Order(-1)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);
    private static final String SERVICE_NAME = "api-gateway";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // Set correlation ID if not present
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-ID");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        MDC.put("requestId", requestId);
        MDC.put("traceId", traceId);

        try {
            HttpStatus status = determineStatus(ex);
            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().set("X-Request-ID", requestId);

            Map<String, Object> errorResponse = buildErrorResponse(status, ex, exchange, requestId, traceId);

            String body = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

            log.error("Gateway error: status={}, path={}, error={}", status.value(),
                    exchange.getRequest().getPath(), ex.getMessage(), ex);

            if (status == HttpStatus.NOT_FOUND) {
                log.warn("No route matched: {} {}", exchange.getRequest().getMethod(),
                        exchange.getRequest().getPath());
            } else {
                log.error("Gateway error: status={}, path={}, error={}",
                        status.value(), exchange.getRequest().getPath(), ex.getMessage(), ex);
            }

            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error writing error response", e);
            return Mono.error(ex);
        } finally {
            MDC.remove("requestId");
            MDC.remove("traceId");
        }
    }

    private HttpStatus determineStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException) {
            return (HttpStatus) ((ResponseStatusException) ex).getStatusCode();
        }
        
        // Handle common downstream service errors
        if (ex.getMessage() != null) {
            String message = ex.getMessage();
            if (message.contains("Connection refused") || message.contains("UnknownHost")) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            if (message.contains("timeout") || message.contains("Timeout")) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private Map<String, Object> buildErrorResponse(HttpStatus status, Throwable ex, ServerWebExchange exchange, 
            String requestId, String traceId) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("service", SERVICE_NAME);
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("code", determineErrorCode(status, ex));
        errorResponse.put("message", determineUserMessage(status, ex));
        errorResponse.put("path", exchange.getRequest().getPath().value());
        errorResponse.put("method", exchange.getRequest().getMethod().name());
        errorResponse.put("traceId", traceId);
        errorResponse.put("requestId", requestId);
        return errorResponse;
    }

    private String determineErrorCode(HttpStatus status, Throwable ex) {
        if (ex instanceof ResponseStatusException) {
            return ((ResponseStatusException) ex).getStatusCode().toString();
        }
        
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return "SERVICE_UNAVAILABLE";
        }
        if (status == HttpStatus.GATEWAY_TIMEOUT) {
            return "GATEWAY_TIMEOUT";
        }
        if (status == HttpStatus.BAD_GATEWAY) {
            return "BAD_GATEWAY";
        }
        
        return "GATEWAY_ERROR";
    }

    private String determineUserMessage(HttpStatus status, Throwable ex) {
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            if (rse.getReason() != null) {
                return rse.getReason();
            }
        }
        
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return "Service is temporarily unavailable. Please try again later.";
        }
        if (status == HttpStatus.GATEWAY_TIMEOUT) {
            return "Request timed out. Please try again.";
        }
        if (status == HttpStatus.BAD_GATEWAY) {
            return "Received invalid response from downstream service.";
        }
        
        return "An unexpected error occurred. Please try again later.";
    }
}
