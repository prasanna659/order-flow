package com.orderflow.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "a-test-secret-that-is-definitely-longer-than-32-bytes!!";

    private JwtUtil util(String secret, String profiles) {
        JwtUtil u = new JwtUtil();
        ReflectionTestUtils.setField(u, "secret", secret);
        ReflectionTestUtils.setField(u, "activeProfiles", profiles);
        ReflectionTestUtils.setField(u, "expirationMs", 60_000L);
        u.init();
        return u;
    }

    @Test
    @DisplayName("issued token carries the username as subject and the user id claim, and is signed with the secret")
    void issuesVerifiableToken() {
        String token = util(SECRET, "").generateToken(42L, "alice");

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("alice", claims.getSubject());
        assertEquals(42, ((Number) claims.get("userId")).intValue());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    @DisplayName("token signed by auth-service is rejected when verified with a different secret")
    void tokenFailsWithWrongSecret() {
        String token = util(SECRET, "").generateToken(1L, "bob");

        assertThrows(io.jsonwebtoken.JwtException.class, () -> Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor("another-secret-that-is-also-longer-than-32-bytes!!".getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token));
    }

    @Test
    @DisplayName("a secret shorter than 32 bytes fails fast at startup")
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class, () -> util("too-short", ""));
    }

    @Test
    @DisplayName("blank secret with the prod profile refuses to start")
    void blankSecretInProdFailsFast() {
        assertThrows(IllegalStateException.class, () -> util("", "prod"));
    }
}
