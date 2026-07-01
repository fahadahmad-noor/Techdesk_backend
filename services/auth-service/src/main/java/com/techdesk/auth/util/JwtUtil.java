package com.techdesk.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

// Stateless JWT helper — only generates and parses tokens, no DB or Redis calls
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // Builds a short-lived access token with userId, tenantId, role, and permissions
    public String generateAccessToken(UUID userId, String tenantId, String role,
                                      List<String> permissions) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId)
                .claim("role", role)
                .claim("permissions", permissions)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    // Builds a long-lived refresh token — role is embedded so rotation doesn't need a DB call
    public String generateRefreshToken(UUID userId, String tenantId, String role, String jti) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId)
                .claim("role", role)
                .claim("type", "refresh")
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    // Parses and verifies the token signature — throws JwtException if invalid or expired
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).getSubject());
    }

    public String extractTenantId(String token) {
        return extractAllClaims(token).get("tenantId", String.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    public String extractType(String token) {
        return extractAllClaims(token).get("type", String.class);
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    // Returns false on any parse error — callers don't need to handle JwtException
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
