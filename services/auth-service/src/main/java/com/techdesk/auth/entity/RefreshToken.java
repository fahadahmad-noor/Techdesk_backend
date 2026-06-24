package com.techdesk.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.UUID;

/**
 * Redis entity representing an active refresh token.
 *
 * Stored as a Redis Hash with key: "refresh_token:<jti>"
 *
 * The `used` flag is the core of replay attack detection:
 * - On first use → set used = true, issue new token pair
 * - On subsequent use → replay attack detected, revoke ALL tokens
 *
 * TTL is set to 7 days (604800 seconds), matching the JWT expiry.
 * Redis automatically deletes the entry when TTL expires.
 */
@RedisHash("refresh_token")
public class RefreshToken {

    @Id
    private String jti;          // JWT ID — used as Redis key

    @Indexed
    private String userId;       // String form of UUID for Redis indexing

    private String tenantId;     // Schema name
    private String role;
    private boolean used;        // Replay attack detection flag

    @TimeToLive
    private long ttl = 604800L;  // 7 days in seconds

    // Getters and Setters

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public long getTtl() { return ttl; }
    public void setTtl(long ttl) { this.ttl = ttl; }
}
