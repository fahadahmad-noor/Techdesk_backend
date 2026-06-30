package com.techdesk.auth.service;

import com.techdesk.auth.dto.request.LoginRequest;
import com.techdesk.auth.dto.response.TokenResponse;
import com.techdesk.auth.entity.User;
import com.techdesk.auth.exception.UserNotFoundException;
import com.techdesk.auth.repository.PasswordResetTokenRepository;
import com.techdesk.auth.repository.UserRepository;
import com.techdesk.auth.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * All dependencies are mocked — no Spring context, no DB, no Redis.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private TokenService tokenService;
    @Mock private EmailService emailService;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User buildUser(String role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@company.com");
        user.setPasswordHash("$2a$12$hashedpassword");
        user.setRole(role);
        user.setStatus("ACTIVE");
        return user;
    }

    // --- Login Tests ---

    @Test
    @DisplayName("Login with valid credentials returns token pair")
    void login_validCredentials_returnsTokenPair() {
        User user = buildUser("EMPLOYEE");
        when(userRepository.findByEmail("test@company.com"))
                .thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(any(), any(), anyString(), any()))
                .thenReturn("access.token");
        when(jwtUtil.generateRefreshToken(any(), any(), anyString(), anyString()))
                .thenReturn("refresh.token");

        LoginRequest req = new LoginRequest();
        req.setEmail("test@company.com");
        req.setPassword("ValidPass1!");

        TokenResponse response = authService.login(req);

        assertNotNull(response);
        assertEquals("access.token", response.getAccessToken());
        assertEquals("refresh.token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());

        // Verify refresh token was saved to Redis
        verify(tokenService).saveRefreshToken(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Login with wrong password throws BadCredentialsException")
    void login_wrongPassword_throwsException() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        LoginRequest req = new LoginRequest();
        req.setEmail("test@company.com");
        req.setPassword("WrongPassword");

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("JWT payload contains userId, tenantId, role, permissions for IT_MANAGER")
    void login_itManager_jwtContainsCorrectClaims() {
        User user = buildUser("IT_MANAGER");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(
                eq(user.getId()),
                any(),
                eq("IT_MANAGER"),
                argThat(perms -> perms.contains("VIEW_ALL_TICKETS"))))
                .thenReturn("access.token");
        when(jwtUtil.generateRefreshToken(any(), any(), anyString(), anyString()))
                .thenReturn("refresh.token");

        LoginRequest req = new LoginRequest();
        req.setEmail("manager@company.com");
        req.setPassword("ValidPass1!");

        authService.login(req);

        // Verify generateAccessToken was called with IT_MANAGER permissions
        verify(jwtUtil).generateAccessToken(
                eq(user.getId()),
                any(),
                eq("IT_MANAGER"),
                argThat(perms -> perms.contains("VIEW_ALL_TICKETS")
                        && perms.contains("APPROVE_GADGETS")));
    }

    // --- Forgot Password Tests ---

    @Test
    @DisplayName("Forgot password for existing email sends reset email")
    void forgotPassword_existingUser_sendsEmail() {
        User user = buildUser("EMPLOYEE");
        when(userRepository.findByEmail("test@company.com"))
                .thenReturn(Optional.of(user));

        var req = new com.techdesk.auth.dto.request.ForgotPasswordRequest();
        req.setEmail("test@company.com");

        var response = authService.forgotPassword(req);

        assertTrue(response.isSuccess());
        verify(emailService).sendPasswordResetEmail(eq("test@company.com"), anyString());
        verify(passwordResetTokenRepository).save(any());
    }

    @Test
    @DisplayName("Forgot password for non-existent email returns same generic message")
    void forgotPassword_nonExistentEmail_returnsGenericMessage() {
        when(userRepository.findByEmail("ghost@company.com")).thenReturn(Optional.empty());

        var req = new com.techdesk.auth.dto.request.ForgotPasswordRequest();
        req.setEmail("ghost@company.com");

        var response = authService.forgotPassword(req);

        // Must still return success=true — don't reveal if email exists
        assertTrue(response.isSuccess());
        verifyNoInteractions(emailService);
    }
}
