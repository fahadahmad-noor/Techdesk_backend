package com.techdesk.auth.service;

import com.techdesk.auth.dto.request.*;
import com.techdesk.auth.dto.response.MessageResponse;
import com.techdesk.auth.dto.response.TokenResponse;
import com.techdesk.auth.entity.PasswordResetToken;
import com.techdesk.auth.entity.User;
import com.techdesk.auth.exception.InvalidTokenException;
import com.techdesk.auth.exception.UserNotFoundException;
import com.techdesk.auth.multitenancy.TenantContextHolder;
import com.techdesk.auth.repository.PasswordResetTokenRepository;
import com.techdesk.auth.repository.UserRepository;
import com.techdesk.auth.util.JwtUtil;
import com.techdesk.auth.util.RolePermissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Core authentication service.
 * Handles all 5 Phase 3.2 endpoint flows.
 * Delegates token lifecycle to TokenService and emails to EmailService.
 *
 * No business logic lives in the controller — only here.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       TokenService tokenService,
                       EmailService emailService,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Authenticates user and returns a JWT token pair.
     * Spring Security's AuthenticationManager verifies email + BCrypt password.
     *
     * @return TokenResponse containing access token (15 min) and refresh token (7 days)
     */
    public TokenResponse login(LoginRequest request) {
        // Delegate credential verification to Spring Security (uses CustomUserDetailsService)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        // Load the user entity to build the JWT payload
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found: " + request.getEmail()));

        String tenantId    = TenantContextHolder.getCurrentSchema();
        String role        = user.getRole();
        List<String> perms = RolePermissionMapper.getPermissions(role);

        // Generate access token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), tenantId, role, perms);

        // Generate refresh token with a UUID jti for Redis storage
        String jti           = UUID.randomUUID().toString();
        String refreshToken  = jwtUtil.generateRefreshToken(user.getId(), tenantId, jti);

        // Save refresh token to Redis
        tokenService.saveRefreshToken(user.getId(), tenantId, role, jti);

        log.info("User {} logged in successfully", user.getEmail());
        return new TokenResponse(accessToken, refreshToken);
    }

    /**
     * Rotates a refresh token — invalidates old one, returns new pair.
     * Detects replay attacks and revokes all sessions if detected.
     */
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        // We need the permissions to generate the new access token.
        // Extract role from the token, then look up permissions.
        // The token is validated inside TokenService.validateAndRotate.
        String role = jwtUtil.extractRole(request.getRefreshToken());
        List<String> perms = RolePermissionMapper.getPermissions(role);

        return tokenService.validateAndRotate(request.getRefreshToken(), perms);
    }

    /**
     * Logs out the user by deleting their refresh token from Redis.
     */
    public MessageResponse logout(RefreshTokenRequest request) {
        tokenService.invalidate(request.getRefreshToken());
        return new MessageResponse("Logged out successfully", true);
    }

    /**
     * Generates a time-limited password reset token and emails it to the user.
     * Deliberately generic response to prevent email enumeration attacks.
     */
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();

            PasswordResetToken resetRecord = new PasswordResetToken();
            resetRecord.setToken(resetToken);
            resetRecord.setUserId(user.getId().toString());
            resetRecord.setEmail(user.getEmail());
            resetRecord.setUsed(false);
            passwordResetTokenRepository.save(resetRecord);

            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
            log.info("Password reset token generated for {}", user.getEmail());
        });

        // Always return the same message — don't reveal if email exists
        return new MessageResponse(
                "If that email is registered, a reset link has been sent.", true);
    }

    /**
     * Validates a reset token and updates the user's password.
     * Token is invalidated after use.
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetRecord = passwordResetTokenRepository
                .findById(request.getToken())
                .orElseThrow(() -> new InvalidTokenException(
                        "Reset token is invalid or has expired"));

        if (resetRecord.isUsed()) {
            throw new InvalidTokenException("Reset token has already been used");
        }

        User user = userRepository.findById(UUID.fromString(resetRecord.getUserId()))
                .orElseThrow(() -> new UserNotFoundException("User no longer exists"));

        // Hash and save the new password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark the reset token as used to prevent re-use
        resetRecord.setUsed(true);
        passwordResetTokenRepository.save(resetRecord);

        // Revoke all active refresh tokens — force re-login
        tokenService.invalidateAllForUser(user.getId());

        log.info("Password reset successful for userId={}", user.getId());
        return new MessageResponse("Password reset successful. Please log in again.", true);
    }
}
