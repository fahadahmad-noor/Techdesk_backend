package com.techdesk.auth.service;

import com.techdesk.auth.dto.response.TokenResponse;
import com.techdesk.auth.entity.RefreshToken;
import com.techdesk.auth.exception.InvalidTokenException;
import com.techdesk.auth.repository.AuditLogRepository;
import com.techdesk.auth.repository.RefreshTokenRepository;
import com.techdesk.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenService.
 *
 * Most critical test: replay attack detection.
 * When a refresh token that has already been used is submitted again,
 * ALL user sessions must be revoked and an exception must be thrown.
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private TokenService tokenService;

    private static final String RAW_TOKEN  = "dummy.refresh.token";
    private static final String JTI        = UUID.randomUUID().toString();
    private static final UUID   USER_ID    = UUID.randomUUID();
    private static final String TENANT_ID  = "tenant_test";
    private static final String ROLE       = "EMPLOYEE";

    @BeforeEach
    void setUpJwtUtil() {
        lenient().when(jwtUtil.extractJti(RAW_TOKEN)).thenReturn(JTI);
        lenient().when(jwtUtil.extractUserId(RAW_TOKEN)).thenReturn(USER_ID);
        lenient().when(jwtUtil.extractTenantId(RAW_TOKEN)).thenReturn(TENANT_ID);
        lenient().when(jwtUtil.extractRole(RAW_TOKEN)).thenReturn(ROLE);
        lenient().when(jwtUtil.extractType(RAW_TOKEN)).thenReturn("refresh");
        lenient().when(jwtUtil.generateAccessToken(any(), anyString(), anyString(), any()))
                .thenReturn("new.access.token");
        lenient().when(jwtUtil.generateRefreshToken(any(), anyString(), anyString()))
                .thenReturn("new.refresh.token");
    }

    // --- Replay Attack Detection Tests ---

    @Test
    @DisplayName("CHALLENGE: Replay attack - used token revokes all user sessions")
    void validateAndRotate_replayAttack_invalidatesAllSessions() {
        RefreshToken usedToken = new RefreshToken();
        usedToken.setJti(JTI);
        usedToken.setUserId(USER_ID.toString());
        usedToken.setUsed(true);  // Already used — replay attack!

        when(refreshTokenRepository.findById(JTI)).thenReturn(Optional.of(usedToken));
        when(refreshTokenRepository.findByUserId(USER_ID.toString()))
                .thenReturn(List.of(usedToken));

        assertThrows(InvalidTokenException.class, () ->
                tokenService.validateAndRotate(RAW_TOKEN, List.of()));

        // Verify all tokens for user were deleted
        verify(refreshTokenRepository).deleteAll(anyList());
    }

    @Test
    @DisplayName("CHALLENGE: Replay attack - security event written to audit log")
    void validateAndRotate_replayAttack_logsSecurityEvent() {
        RefreshToken usedToken = new RefreshToken();
        usedToken.setJti(JTI);
        usedToken.setUserId(USER_ID.toString());
        usedToken.setUsed(true);

        when(refreshTokenRepository.findById(JTI)).thenReturn(Optional.of(usedToken));
        when(refreshTokenRepository.findByUserId(USER_ID.toString()))
                .thenReturn(List.of(usedToken));

        assertThrows(InvalidTokenException.class, () ->
                tokenService.validateAndRotate(RAW_TOKEN, List.of()));

        // Verify the security event was written to audit_logs
        verify(auditLogRepository, atLeastOnce()).save(any());
    }

    // --- Normal Rotation Tests ---

    @Test
    @DisplayName("Valid token rotation returns new token pair")
    void validateAndRotate_validToken_returnsNewTokenPair() {
        RefreshToken freshToken = new RefreshToken();
        freshToken.setJti(JTI);
        freshToken.setUserId(USER_ID.toString());
        freshToken.setUsed(false);  // Not used — legitimate request

        when(refreshTokenRepository.findById(JTI)).thenReturn(Optional.of(freshToken));

        TokenResponse result = tokenService.validateAndRotate(RAW_TOKEN, List.of());

        assertNotNull(result);
        assertEquals("new.access.token", result.getAccessToken());
        assertEquals("new.refresh.token", result.getRefreshToken());
    }

    @Test
    @DisplayName("Valid token rotation marks old token as used")
    void validateAndRotate_validToken_marksOldTokenUsed() {
        RefreshToken freshToken = new RefreshToken();
        freshToken.setJti(JTI);
        freshToken.setUserId(USER_ID.toString());
        freshToken.setUsed(false);

        when(refreshTokenRepository.findById(JTI)).thenReturn(Optional.of(freshToken));

        tokenService.validateAndRotate(RAW_TOKEN, List.of());

        assertTrue(freshToken.isUsed());
        verify(refreshTokenRepository, atLeastOnce()).save(freshToken);
    }

    @Test
    @DisplayName("Non-existent token throws InvalidTokenException")
    void validateAndRotate_tokenNotInRedis_throwsException() {
        when(refreshTokenRepository.findById(JTI)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () ->
                tokenService.validateAndRotate(RAW_TOKEN, List.of()));
    }

    // --- Invalidation Tests ---

    @Test
    @DisplayName("Logout deletes token from Redis by jti")
    void invalidate_deletesTokenByJti() {
        tokenService.invalidate(RAW_TOKEN);
        verify(refreshTokenRepository).deleteById(JTI);
    }

    @Test
    @DisplayName("invalidateAllForUser deletes all tokens for the user")
    void invalidateAllForUser_deletesAllUserTokens() {
        RefreshToken t1 = new RefreshToken();
        RefreshToken t2 = new RefreshToken();
        when(refreshTokenRepository.findByUserId(USER_ID.toString()))
                .thenReturn(List.of(t1, t2));

        tokenService.invalidateAllForUser(USER_ID);

        verify(refreshTokenRepository).deleteAll(List.of(t1, t2));
    }
}
