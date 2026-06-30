package com.techdesk.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

/**
 * Redis entity for password reset tokens.
 * Stored as: "password_reset_token:<token>"
 * TTL: 15 minutes (900 seconds), matching the forgot-password flow requirement.
 */
@RedisHash("password_reset_token")
public class PasswordResetToken {

    @Id
    private String token;        // UUID string used as Redis key

    private String userId;       // String form of UUID
    private String email;        // User's email (for validation)
    private boolean used;        // Prevents token re-use after reset

    @TimeToLive
    private long ttl = 900L;     // 15 minutes in seconds

    // Getters and Setters

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public long getTtl() { return ttl; }
    public void setTtl(long ttl) { this.ttl = ttl; }
}
