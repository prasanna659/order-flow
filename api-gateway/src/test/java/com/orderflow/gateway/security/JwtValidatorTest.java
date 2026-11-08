package com.orderflow.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtValidatorTest {

    private static final String SECRET = "a-test-secret-that-is-definitely-longer-than-32-bytes!!";

    private JwtValidator validatorWith(String secret) {
        JwtValidator v = new JwtValidator();
        ReflectionTestUtils.setField(v, "secret", secret);
        v.init();
        return v;
    }

    private String token(String secret, long userId, String username, long expiresInMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiresInMs))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("a valid token yields its user id and username")
    void validTokenIsAccepted() {
        JwtValidator v = validatorWith(SECRET);

        Claims claims = v.validateAndGetClaims(token(SECRET, 42L, "alice", 60_000));

        assertEquals("alice", claims.getSubject());
        assertEquals(42, ((Number) claims.get("userId")).intValue());
    }

    @Test
    @DisplayName("a token signed with a different secret is rejected")
    void wrongSignatureIsRejected() {
        JwtValidator v = validatorWith(SECRET);
        String forged = token("a-completely-different-secret-also-over-32-bytes!!", 1L, "mallory", 60_000);

        assertThrows(JwtValidator.InvalidTokenException.class, () -> v.validateAndGetClaims(forged));
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredTokenIsRejected() {
        JwtValidator v = validatorWith(SECRET);

        assertThrows(JwtValidator.InvalidTokenException.class,
                () -> v.validateAndGetClaims(token(SECRET, 1L, "alice", -60_000)));
    }

    @Test
    @DisplayName("garbage is rejected")
    void garbageIsRejected() {
        JwtValidator v = validatorWith(SECRET);

        assertThrows(JwtValidator.InvalidTokenException.class, () -> v.validateAndGetClaims("not.a.jwt"));
    }

    @Test
    @DisplayName("a secret shorter than 32 bytes fails fast at startup")
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class, () -> validatorWith("too-short"));
    }

    @Test
    @DisplayName("blank secret with the prod profile active refuses to start (public dev key must not be used)")
    void blankSecretInProdFailsFast() {
        JwtValidator v = new JwtValidator();
        ReflectionTestUtils.setField(v, "secret", "");
        ReflectionTestUtils.setField(v, "activeProfiles", "prod");

        assertThrows(IllegalStateException.class, v::init);
    }

    @Test
    @DisplayName("blank secret outside prod falls back to the dev key so local runs still work")
    void blankSecretInDevFallsBack() {
        JwtValidator v = new JwtValidator();
        ReflectionTestUtils.setField(v, "secret", "");
        ReflectionTestUtils.setField(v, "activeProfiles", "");

        v.init(); // must not throw
    }
}
