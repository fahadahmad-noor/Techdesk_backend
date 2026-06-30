package com.techdesk.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity that maps to the public.tenants table.
 *
 * This table is the central registry of all companies (tenants) on the TechDesk platform.
 * It lives in the public schema and is readable by the Super Admin to see which companies
 * are subscribed. Each company has a unique schema_name pointing to their isolated database schema.
 */
@Entity
@Table(name = "tenants", schema = "public")
public class Tenant {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    /**
     * Human-readable company name as provided during onboarding (e.g., "Acme Corp").
     * Must be unique across all tenants on the platform.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * The PostgreSQL schema name allocated exclusively for this tenant.
     * Derived from the company name during creation (e.g., "tenant_acme_corp").
     * Used by all downstream services to route database queries to the correct schema.
     */
    @Column(name = "schema_name", nullable = false, unique = true, length = 63)
    private String schemaName;

    /**
     * Lifecycle status of the tenant account.
     * PENDING   — creation is in progress.
     * ACTIVE    — fully operational and accessible.
     * SUSPENDED — account deactivated (e.g., subscription lapsed).
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TenantStatus status;

    /**
     * Subscription plan tier selected by the company at onboarding.
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TenantPlan plan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }

    public TenantPlan getPlan() { return plan; }
    public void setPlan(TenantPlan plan) { this.plan = plan; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
