package com.orderflow.auth.controller;

import com.orderflow.auth.dto.ForgotPasswordRequest;
import com.orderflow.auth.dto.ResetPasswordRequest;
import com.orderflow.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Password Reset", description = "Forgot password and reset password endpoints")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * POST /api/auth/forgot-password
     *
     * Always returns 200 regardless of whether the email exists.
     * This prevents user enumeration attacks — the caller cannot determine
     * from the response whether an account exists for that email.
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.forgotPassword(req.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "If an account exists for that email, a reset link has been sent"));
    }

    /**
     * GET /api/auth/reset-password/validate?token=...
     *
     * Validates the token without consuming it — lets the frontend show the
     * new-password form only if the token is still valid, rather than
     * discovering it's invalid after the user types a new password.
     */
    @GetMapping("/reset-password/validate")
    @Operation(summary = "Validate a password reset token")
    public ResponseEntity<Map<String, String>> validateToken(@RequestParam String token) {
        passwordResetService.validateToken(token);
        return ResponseEntity.ok(Map.of("message", "Token is valid"));
    }

    /**
     * POST /api/auth/reset-password
     *
     * Validates the token, saves the new bcrypt-encoded password,
     * and invalidates the token so it cannot be reused.
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a valid token")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }
}
