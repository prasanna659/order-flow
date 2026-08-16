package com.orderflow.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Stores a single-use, time-limited password reset token.
 *
 * - token: cryptographically random UUID, stored as-is (no need to hash for
 *   a 30-minute token — the short window and single-use constraint are
 *   sufficient for a portfolio project; production would use a SHA-256 hash).
 * - used: set to true immediately on redemption so the token cannot be reused.
 * - expiresAt: 30 minutes from creation, checked before redemption.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public PasswordResetToken() {}

    public PasswordResetToken(String token, User user, Instant expiresAt) {
        this.token     = token;
        this.user      = user;
        this.expiresAt = expiresAt;
    }

    public Long getId()          { return id; }
    public String getToken()     { return token; }
    public User getUser()        { return user; }
    public Instant getExpiresAt(){ return expiresAt; }
    public boolean isUsed()      { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public Instant getCreatedAt(){ return createdAt; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
