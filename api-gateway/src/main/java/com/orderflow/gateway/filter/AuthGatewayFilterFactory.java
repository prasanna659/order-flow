package com.orderflow.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.gateway.security.JwtValidator;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Applied per-route in application.yml as "filters: [Auth]". Routes
// without this filter (auth-service, product browsing) stay fully public.
// This is where "protected route" actually gets enforced -- every
// downstream service trusts that if a request reached it through the
// gateway with an X-User-Id header, the gateway already verified the JWT.
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final JwtValidator jwtValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthGatewayFilterFactory(JwtValidator jwtValidator) {
        super(Object.class);
        this.jwtValidator = jwtValidator;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return writeErrorResponse(exchange.getResponse(), HttpStatus.UNAUTHORIZED, 
                        "MISSING_TOKEN", "Authorization header is missing or invalid");
            }

            try {
                String token = authHeader.substring(7);
                Claims claims = jwtValidator.validateAndGetClaims(token);

                // Forward identity downstream as a trusted header so
                // order-service etc. don't need to re-parse or even see
                // the JWT itself.
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", String.valueOf(claims.get("userId")))
                        .header("X-Username", claims.getSubject())
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (JwtValidator.InvalidTokenException e) {
                return writeErrorResponse(exchange.getResponse(), HttpStatus.UNAUTHORIZED, 
                        "INVALID_TOKEN", "Invalid or expired JWT token");
            }
        };
    }

    private Mono<Void> writeErrorResponse(ServerHttpResponse response, HttpStatus status, 
            String code, String message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String requestId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        response.getHeaders().set("X-Request-ID", requestId);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("service", "api-gateway");
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("code", code);
        errorResponse.put("message", message);
        errorResponse.put("path", "/api/auth/**");
        errorResponse.put("method", "*");
        errorResponse.put("traceId", traceId);
        errorResponse.put("requestId", requestId);
        
        try {
            String body = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }
}
