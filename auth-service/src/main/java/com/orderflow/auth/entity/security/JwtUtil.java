package com.orderflow.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// This class exists in BOTH auth-service (to issue tokens) and api-gateway
// (to validate them). They share the same jwt.secret value — that's what lets
// the gateway verify a token it never issued itself.
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // Minimum 32 characters required for HMAC-SHA256 (256 bits).
    // The fallback here is a dev-only placeholder — in production JWT_SECRET
    // MUST be set in Railway Variables to a cryptographically random string
    // of at least 64 characters.
    private static final String DEV_FALLBACK =
            "orderflow-dev-secret-key-change-in-production-min32chars!";

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    private SecretKey cachedKey;

    @PostConstruct
    public void init() {
        // Validate and cache the key at startup so a misconfigured secret
        // fails fast with a clear message rather than blowing up mid-request.
        if (secret == null || secret.isBlank()) {
            if (activeProfiles != null && java.util.Arrays.asList(activeProfiles.split(",")).contains("prod")) {
                throw new IllegalStateException(
                    "JWT_SECRET must be set (min 32 chars) when the prod profile is active. "
                    + "The built-in dev fallback is public and must never be used in production.");
            }
            log.warn("JWT_SECRET is not set — using dev fallback key. " +
                     "Set JWT_SECRET in Railway Variables before going to production.");
            secret = DEV_FALLBACK;
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT secret is too short (" + keyBytes.length + " bytes). " +
                "Minimum 32 bytes (256 bits) required for HMAC-SHA256. " +
                "Set a longer JWT_SECRET in your environment variables.");
        }
        cachedKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT key initialised ({} bytes)", keyBytes.length);
    }

    public String generateToken(Long userId, String username) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(cachedKey)
                .compact();
    }
}
