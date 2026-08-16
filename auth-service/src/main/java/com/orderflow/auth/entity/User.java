package com.orderflow.auth.entity;

import jakarta.persistence.*;

/**
 * Single user table for both local (username/password) and OAuth2 (Google) accounts.
 *
 * Local accounts:   provider = "local",  googleId = null, password = bcrypt hash
 * Google accounts:  provider = "google", googleId = sub,  password = "" (never used)
 *
 * The unique constraint on email means the same email address cannot be used
 * for both a local and a Google account simultaneously — intentional for this
 * portfolio scope (in production you'd handle account linking separately).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    // Blank string for Google-only accounts — never matched against a real password.
    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    // ── OAuth2 fields (nullable — not set for local accounts) ──────────
    @Column(name = "google_id", unique = true)
    private String googleId;

    /** Display name from Google profile */
    private String name;

    /** Profile picture URL from Google */
    @Column(name = "picture_url", length = 512)
    private String pictureUrl;

    /**
     * Authentication provider: "local" or "google".
     * Defaults to "local" for backwards compatibility with existing rows.
     */
    @Column(nullable = false)
    private String provider = "local";

    public User() {}

    /** Constructor for local username/password registration */
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email    = email;
        this.provider = "local";
    }

    public Long getId()           { return id; }
    public String getUsername()   { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword()   { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail()      { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGoogleId()   { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }
    public String getName()       { return name; }
    public void setName(String name) { this.name = name; }
    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }
    public String getProvider()   { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
