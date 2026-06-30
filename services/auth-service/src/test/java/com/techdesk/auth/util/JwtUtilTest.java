package com.techdesk.auth.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil — no Spring context required.
 * Tests token generation, claim extraction, and expiry detection.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET =
            "supercalifragilisticexpialidociousmysticalsecretsignkeytechdesk2026";
    private static final long ACCESS_EXP  = 900_000L;   // 15 min
    private static final long REFRESH_EXP = 604_800_000L; // 7 days

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, ACCESS_EXP, REFRESH_EXP);
    }

    @Test
    @DisplayName("Access token contains correct userId claim")
    void accessToken_containsUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(
                userId, "tenant_test", "EMPLOYEE", List.of("CREATE_TICKET"));

        assertEquals(userId, jwtUtil.extractUserId(token));
    }

    @Test
    @DisplayName("Access token contains correct tenantId claim")
    void accessToken_containsTenantId() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(
                userId, "tenant_companya", "IT_STAFF", List.of());

        assertEquals("tenant_companya", jwtUtil.extractTenantId(token));
    }

    @Test
    @DisplayName("Access token contains correct role claim")
    void accessToken_containsRole() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(
                userId, "tenant_test", "IT_MANAGER", List.of());

        assertEquals("IT_MANAGER", jwtUtil.extractRole(token));
    }

    @Test
    @DisplayName("Access token type claim is 'access'")
    void accessToken_typeIsAccess() {
        String token = jwtUtil.generateAccessToken(
                UUID.randomUUID(), "tenant_test", "EMPLOYEE", List.of());

        assertEquals("access", jwtUtil.extractType(token));
    }

    @Test
    @DisplayName("Refresh token type claim is 'refresh'")
    void refreshToken_typeIsRefresh() {
        String jti = UUID.randomUUID().toString();
        String token = jwtUtil.generateRefreshToken(
                UUID.randomUUID(), "tenant_test", "EMPLOYEE", jti);

        assertEquals("refresh", jwtUtil.extractType(token));
    }

    @Test
    @DisplayName("Refresh token contains correct jti claim")
    void refreshToken_containsJti() {
        String jti = UUID.randomUUID().toString();
        String token = jwtUtil.generateRefreshToken(
                UUID.randomUUID(), "tenant_test", "SUPER_ADMIN", jti);

        assertEquals(jti, jwtUtil.extractJti(token));
    }

    @Test
    @DisplayName("Valid token is recognized as valid")
    void isTokenValid_returnsTrue_forValidToken() {
        String token = jwtUtil.generateAccessToken(
                UUID.randomUUID(), "tenant_test", "EMPLOYEE", List.of());

        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    @DisplayName("Expired token is recognized as invalid")
    void isTokenValid_returnsFalse_forExpiredToken() {
        // Create a JwtUtil with -1ms expiration (already expired)
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, -1L, -1L);
        String token = expiredJwtUtil.generateAccessToken(
                UUID.randomUUID(), "tenant_test", "EMPLOYEE", List.of());

        assertFalse(expiredJwtUtil.isTokenValid(token));
    }

    @Test
    @DisplayName("Tampered token is recognized as invalid")
    void isTokenValid_returnsFalse_forTamperedToken() {
        String token = jwtUtil.generateAccessToken(
                UUID.randomUUID(), "tenant_test", "EMPLOYEE", List.of());
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";

        assertFalse(jwtUtil.isTokenValid(tampered));
    }
}
