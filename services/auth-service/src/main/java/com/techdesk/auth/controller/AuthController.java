package com.techdesk.auth.controller;

import com.techdesk.auth.dto.request.*;
import com.techdesk.auth.dto.response.MessageResponse;
import com.techdesk.auth.dto.response.TokenResponse;
import com.techdesk.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 *
 * IMPORTANT: Zero business logic lives here.
 * This class only receives HTTP requests, delegates to AuthService, and returns responses.
 * All validation is handled by @Valid + GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/login
     * Authenticates user and returns access + refresh JWT pair.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/auth/refresh-token
     * Rotates refresh token (one-time use). Detects replay attacks.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<TokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * POST /api/auth/logout
     * Invalidates the provided refresh token in Redis.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.logout(request));
    }

    /**
     * POST /api/auth/forgot-password
     * Generates a time-limited reset token and sends email.
     * Always returns the same message to prevent email enumeration.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    /**
     * POST /api/auth/reset-password
     * Validates the reset token, updates password, and revokes all sessions.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
