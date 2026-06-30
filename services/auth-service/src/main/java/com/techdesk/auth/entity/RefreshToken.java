package com.techdesk.auth.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.UUID;

// Redis entity for an active refresh token — key: "refresh_token:<jti>", TTL: 7 days
// used=true means the token was already consumed; a second use = replay attack
@RedisHash("refresh_token")
public class RefreshToken {

    @Id
    private String jti;

    @Indexed
    private String userId;

    private String tenantId;
    private String role;
    private boolean used;

    @TimeToLive
    private long ttl = 604800L; // 7 days in seconds

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
