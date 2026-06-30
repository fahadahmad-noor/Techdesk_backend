package com.techdesk.auth.service;

import com.techdesk.auth.dto.response.TokenResponse;
import com.techdesk.auth.entity.AuditLog;
import com.techdesk.auth.entity.RefreshToken;
import com.techdesk.auth.exception.InvalidTokenException;
import com.techdesk.auth.repository.AuditLogRepository;
import com.techdesk.auth.repository.RefreshTokenRepository;
import com.techdesk.auth.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Handles refresh token storage, rotation, and replay attack detection
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;

    public TokenService(JwtUtil jwtUtil,
                        RefreshTokenRepository refreshTokenRepository,
                        AuditLogRepository auditLogRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // Persists a new refresh token to Redis after login
    public void saveRefreshToken(UUID userId, String tenantId, String role, String jti) {
        RefreshToken token = new RefreshToken();
        token.setJti(jti);
        token.setUserId(userId.toString());
        token.setTenantId(tenantId);
        token.setRole(role);
        token.setUsed(false);
        refreshTokenRepository.save(token);
        log.debug("Saved refresh token jti={} for userId={}", jti, userId);
    }

    // Validates and rotates the refresh token — if already used, all sessions are revoked
    public TokenResponse validateAndRotate(String rawRefreshToken, List<String> permissions) {
        String jti;
        UUID userId;
        String tenantId;
        String role;

        try {
            jti      = jwtUtil.extractJti(rawRefreshToken);
            userId   = jwtUtil.extractUserId(rawRefreshToken);
            tenantId = jwtUtil.extractTenantId(rawRefreshToken);
            role     = jwtUtil.extractRole(rawRefreshToken);

            if (!"refresh".equals(jwtUtil.extractType(rawRefreshToken))) {
                throw new InvalidTokenException("Provided token is not a refresh token");
            }
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        RefreshToken stored = refreshTokenRepository.findById(jti)
                .orElseThrow(() -> new InvalidTokenException(
                        "Refresh token not found or already expired"));

        // If token was already used, someone is replaying it — kill all sessions immediately
        if (stored.isUsed()) {
            log.error("REPLAY ATTACK DETECTED — userId={}, jti={}", userId, jti);
            invalidateAllForUser(userId);
            logSecurityEvent(userId, tenantId, "SECURITY_EVENT_REPLAY_ATTACK",
                    "REFRESH_TOKEN", jti);
            throw new InvalidTokenException(
                    "Security violation detected. All sessions have been revoked. "
                    + "Please log in again.");
        }

        stored.setUsed(true);
        refreshTokenRepository.save(stored);

        String newJti          = UUID.randomUUID().toString();
        String newAccessToken  = jwtUtil.generateAccessToken(userId, tenantId, role, permissions);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, tenantId, role, newJti);

        saveRefreshToken(userId, tenantId, role, newJti);

        log.debug("Rotated refresh token for userId={}", userId);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    // Deletes the refresh token from Redis on logout
    public void invalidate(String rawRefreshToken) {
        try {
            String jti = jwtUtil.extractJti(rawRefreshToken);
            refreshTokenRepository.deleteById(jti);
            log.debug("Invalidated refresh token jti={}", jti);
        } catch (JwtException e) {
            log.warn("Attempted to invalidate malformed token: {}", e.getMessage());
        }
    }

    // Nuclear option — wipes all active sessions for a user (used on replay attack)
    public void invalidateAllForUser(UUID userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId.toString());
        refreshTokenRepository.deleteAll(tokens);
        log.warn("Revoked {} active sessions for userId={}", tokens.size(), userId);
    }

    // Writes a security event to audit_logs — will be replaced with ApplicationEvent in Phase 6.3
    private void logSecurityEvent(UUID actorId, String tenantId,
                                  String action, String entityType, String entityId) {
        try {
            AuditLog event = new AuditLog();
            event.setId(UUID.randomUUID());
            event.setActorId(actorId);
            event.setTenantId(tenantId);
            event.setAction(action);
            event.setEntityType(entityType);
            event.setEntityId(UUID.fromString(entityId.replace("-", "")
                    .replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")));
            event.setTimestamp(LocalDateTime.now());
            auditLogRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to write security event to audit log", e);
        }
    }
}
