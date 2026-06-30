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

/**
 * Manages the full lifecycle of refresh tokens in Redis.
 *
 * Core responsibility: refresh token rotation + replay attack detection.
 *
 * Replay Attack Flow:
 *  1. Client sends a refresh token.
 *  2. We look it up in Redis by its jti claim.
 *  3. If used == true → replay attack:
 *       a. Delete ALL tokens for this user from Redis (nuclear option)
 *       b. Write SECURITY_EVENT_REPLAY_ATTACK to public.audit_logs (Phase 3.2 shortcut)
 *       c. Throw InvalidTokenException → user must log in again
 *  4. If used == false → legitimate request:
 *       a. Mark this token as used = true in Redis
 *       b. Generate a new token pair
 *       c. Save new refresh token to Redis
 *       d. Return new TokenResponse
 *
 * TODO Phase 6.3: Replace auditLogRepository.save() with ApplicationEvent publishing.
 */
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

    /**
     * Saves a new refresh token to Redis after successful login.
     *
     * @param userId   user's UUID
     * @param tenantId schema name
     * @param role     user's role
     * @param jti      the JWT ID claim — used as the Redis key
     */
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

    /**
     * Validates a refresh token and rotates it (one-time use).
     * Detects replay attacks and responds with full session revocation.
     *
     * @param rawRefreshToken the raw JWT refresh token string from the client
     * @return a new TokenResponse with fresh access + refresh tokens
     */
    public TokenResponse validateAndRotate(String rawRefreshToken,
                                           List<String> permissions) {
        // Step 1: Parse the JWT to get the jti
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

        // Step 2: Look up the token in Redis
        RefreshToken stored = refreshTokenRepository.findById(jti)
                .orElseThrow(() -> new InvalidTokenException(
                        "Refresh token not found or already expired"));

        // Step 3: Replay attack detection
        if (stored.isUsed()) {
            log.error("REPLAY ATTACK DETECTED — userId={}, jti={}", userId, jti);
            invalidateAllForUser(userId);
            logSecurityEvent(userId, tenantId, "SECURITY_EVENT_REPLAY_ATTACK",
                    "REFRESH_TOKEN", jti);
            throw new InvalidTokenException(
                    "Security violation detected. All sessions have been revoked. "
                    + "Please log in again.");
        }

        // Step 4: Mark the current token as used
        stored.setUsed(true);
        refreshTokenRepository.save(stored);

        // Step 5: Generate new token pair
        String newJti          = UUID.randomUUID().toString();
        String newAccessToken  = jwtUtil.generateAccessToken(userId, tenantId, role, permissions);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, tenantId, newJti);

        // Step 6: Save new refresh token to Redis
        saveRefreshToken(userId, tenantId, role, newJti);

        log.debug("Rotated refresh token for userId={}", userId);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    /**
     * Invalidates a refresh token on logout.
     * Deletes from Redis so it cannot be used again.
     *
     * @param rawRefreshToken the raw JWT refresh token string
     */
    public void invalidate(String rawRefreshToken) {
        try {
            String jti = jwtUtil.extractJti(rawRefreshToken);
            refreshTokenRepository.deleteById(jti);
            log.debug("Invalidated refresh token jti={}", jti);
        } catch (JwtException e) {
            log.warn("Attempted to invalidate malformed token: {}", e.getMessage());
            // Silently ignore — the token is already unusable
        }
    }

    /**
     * Deletes ALL refresh tokens for a user from Redis.
     * Called during replay attack — revokes every active session.
     */
    public void invalidateAllForUser(UUID userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId.toString());
        refreshTokenRepository.deleteAll(tokens);
        log.warn("Revoked {} active sessions for userId={}", tokens.size(), userId);
    }

    /**
     * Writes a security event directly to public.audit_logs.
     * TODO Phase 6.3: Replace with ApplicationEvent publishing.
     */
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
            // Never let audit logging failure break the main flow
            log.error("Failed to write security event to audit log", e);
        }
    }
}
