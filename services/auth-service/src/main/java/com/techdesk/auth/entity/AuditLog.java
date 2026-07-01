package com.techdesk.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

// Maps to public.audit_logs — always public schema, never tenant schema
// TODO Phase 6.3: Replace direct writes with ApplicationEvent publishing to audit-service
@Entity
@Table(name = "audit_logs", schema = "public")
public class AuditLog {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 100)
    private String actorEmail;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public String getActorEmail() { return actorEmail; }
    public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
