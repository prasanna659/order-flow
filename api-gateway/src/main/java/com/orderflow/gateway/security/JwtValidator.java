package com.orderflow.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

// Same secret as auth-service, pulled from the same Railway environment variable.
// The gateway never talks to auth-service to validate a token — JWTs are
// self-contained and verified locally with just the shared secret.
@Component
public class JwtValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtValidator.class);

    private static final String DEV_FALLBACK =
            "orderflow-dev-secret-key-change-in-production-min32chars!";

    @Value("${jwt.secret:}")
    private String secret;

    private SecretKey cachedKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET is not set in api-gateway — using dev fallback key. " +
                     "Ensure JWT_SECRET matches the auth-service value in Railway Variables.");
            secret = DEV_FALLBACK;
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT secret is too short (" + keyBytes.length + " bytes). " +
                "Minimum 32 bytes required. Set JWT_SECRET in Railway Variables.");
        }
        cachedKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT validator key initialised ({} bytes)", keyBytes.length);
    }

    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(cachedKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired token: " + e.getMessage());
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) { super(message); }
    }
}
