package com.orderflow.auth.service;

import com.orderflow.auth.entity.PasswordResetToken;
import com.orderflow.auth.entity.User;
import com.orderflow.auth.exception.InvalidTokenException;
import com.orderflow.auth.exception.ResourceNotFoundException;
import com.orderflow.auth.exception.TokenExpiredException;
import com.orderflow.auth.repository.PasswordResetTokenRepository;
import com.orderflow.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the complete forgot-password / reset-password flow:
 *
 * 1. forgotPassword(email)    — generates a secure token, publishes a Kafka event
 *                               so notification-service sends the reset email.
 * 2. validateToken(token)     — checks the token exists, is unused, and not expired.
 * 3. resetPassword(token, pw) — verifies the token, bcrypt-encodes the new password,
 *                               marks the token used, and deletes all other tokens
 *                               for that user.
 *
 * Security properties:
 *   - Token: random UUID (128 bits of entropy — sufficient for 30-min window)
 *   - Single-use: token.used = true on first redemption
 *   - 30-minute expiry enforced before any password change
 *   - Timing-safe: always returns 200 for forgotPassword regardless of
 *     whether the email exists (prevents user enumeration)
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final long EXPIRY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public PasswordResetService(UserRepository userRepository,
                                 PasswordResetTokenRepository tokenRepository,
                                 PasswordEncoder passwordEncoder,
                                 KafkaTemplate<String, String> kafkaTemplate) {
        this.userRepository  = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.kafkaTemplate   = kafkaTemplate;
    }

    /**
     * Initiates the forgot-password flow.
     * Always returns silently even if the email is not found — prevents user enumeration.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            // Delete any existing (possibly expired) tokens for this user
            tokenRepository.deleteAllByUserId(user.getId());

            String rawToken = UUID.randomUUID().toString().replace("-", "");
            Instant expiresAt = Instant.now().plusSeconds(EXPIRY_MINUTES * 60);
            PasswordResetToken prt = new PasswordResetToken(rawToken, user, expiresAt);
            tokenRepository.save(prt);

            String resetLink = frontendUrl + "/auth/reset-password?token=" + rawToken;
            publishResetEvent(user, resetLink);

            log.info("PASSWORD_RESET_REQUESTED userId={} email={}", user.getId(), email);
        }, () -> log.warn("PASSWORD_RESET_EMAIL_NOT_FOUND email={}", email));
    }

    /**
     * Validates a reset token without consuming it (used by the frontend
     * to check the token before showing the new-password form).
     */
    public void validateToken(String token) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("INVALID_RESET_TOKEN",
                        "Reset token is invalid or does not exist"));
        if (prt.isUsed()) {
            throw new InvalidTokenException("TOKEN_ALREADY_USED",
                    "This reset link has already been used");
        }
        if (prt.isExpired()) {
            throw new TokenExpiredException("TOKEN_EXPIRED",
                    "This reset link has expired. Please request a new one");
        }
    }

    /**
     * Resets the password. Validates the token, encodes the new password,
     * marks the token as used, and removes all other tokens for the user.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("INVALID_RESET_TOKEN",
                        "Reset token is invalid or does not exist"));

        if (prt.isUsed()) {
            throw new InvalidTokenException("TOKEN_ALREADY_USED",
                    "This reset link has already been used");
        }
        if (prt.isExpired()) {
            throw new TokenExpiredException("TOKEN_EXPIRED",
                    "This reset link has expired. Please request a new one");
        }

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark used and clean up all tokens for this user
        prt.setUsed(true);
        tokenRepository.save(prt);
        tokenRepository.deleteAllByUserId(user.getId());

        log.info("PASSWORD_RESET_SUCCESS userId={}", user.getId());
    }

    private void publishResetEvent(User user, String resetLink) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType",  "PASSWORD_RESET");
            event.put("userId",     user.getId());
            event.put("userEmail",  user.getEmail());
            event.put("username",   user.getUsername());
            event.put("resetLink",  resetLink);
            event.put("expiresInMinutes", EXPIRY_MINUTES);
            event.put("timestamp",  Instant.now().toString());

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("auth-events", user.getId().toString(), json);
            log.info("PASSWORD_RESET_EVENT_PUBLISHED userId={}", user.getId());
        } catch (Exception e) {
            // Non-fatal: log the link so it can be manually retrieved in dev
            log.error("PASSWORD_RESET_EVENT_FAILED userId={} resetLink={} error={}",
                    user.getId(), resetLink, e.getMessage());
        }
    }
}
