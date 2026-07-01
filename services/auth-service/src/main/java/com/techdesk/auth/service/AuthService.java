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

// All authentication business logic lives here — controller just delegates
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

    // Verifies credentials via Spring Security then returns a JWT access + refresh token pair
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found: " + request.getEmail()));

        String tenantId    = TenantContextHolder.getCurrentSchema();
        String role        = user.getRole();
        List<String> perms = RolePermissionMapper.getPermissions(role);

        String accessToken  = jwtUtil.generateAccessToken(user.getId(), tenantId, role, perms);
        String jti          = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), tenantId, role, jti);

        tokenService.saveRefreshToken(user.getId(), tenantId, role, jti);

        log.info("User {} logged in successfully", user.getEmail());
        return new TokenResponse(accessToken, refreshToken);
    }

    // Rotates the refresh token and returns a new token pair — replay detection handled inside TokenService
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String role = jwtUtil.extractRole(request.getRefreshToken());
        List<String> perms = RolePermissionMapper.getPermissions(role);
        return tokenService.validateAndRotate(request.getRefreshToken(), perms);
    }

    // Removes the refresh token from Redis — access token expires naturally
    public MessageResponse logout(RefreshTokenRequest request) {
        tokenService.invalidate(request.getRefreshToken());
        return new MessageResponse("Logged out successfully", true);
    }

    // Sends a password reset link — always returns the same message to prevent email enumeration
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

        return new MessageResponse(
                "If that email is registered, a reset link has been sent.", true);
    }

    // Validates the reset token, updates the password, and forces re-login on all devices
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

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetRecord.setUsed(true);
        passwordResetTokenRepository.save(resetRecord);

        // Revoke all sessions so the user has to log in with the new password
        tokenService.invalidateAllForUser(user.getId());

        log.info("Password reset successful for userId={}", user.getId());
        return new MessageResponse("Password reset successful. Please log in again.", true);
    }
}
