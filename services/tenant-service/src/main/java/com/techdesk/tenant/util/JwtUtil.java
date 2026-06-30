package com.techdesk.tenant.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Stateless utility for parsing and validating incoming JWT access tokens.
 *
 * The tenant-service does not issue tokens — that is the sole responsibility of the auth-service.
 * This utility only verifies that an incoming token is legitimate (correct signature, not expired)
 * and extracts the claims needed for authorization (role, userId, tenantId).
 *
 * The JWT secret must match the secret used by the auth-service to sign the tokens.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses the JWT and returns all claims embedded in the payload.
     * Throws a JwtException if the token is expired, malformed, or has an invalid signature.
     *
     * @param token the raw JWT string (without the "Bearer " prefix)
     * @return the full Claims object containing userId, tenantId, role, and permissions
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the role claim from a validated JWT.
     *
     * @param token the raw JWT string
     * @return the role string (e.g., "SUPER_ADMIN", "COMPANY_ADMIN")
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Extracts the userId claim from a validated JWT.
     *
     * @param token the raw JWT string
     * @return the userId as a String representation of the UUID
     */
    public String extractUserId(String token) {
        return extractAllClaims(token).get("userId", String.class);
    }
}
