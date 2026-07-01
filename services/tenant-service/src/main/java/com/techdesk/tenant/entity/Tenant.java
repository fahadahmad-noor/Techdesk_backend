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

// Maps to public.tenants — the platform-level registry of all onboarded companies
@Entity
@Table(name = "tenants", schema = "public")
public class Tenant {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // Derived from the company name e.g. "Acme Corp" -> "tenant_acme_corp"
    @Column(name = "schema_name", nullable = false, unique = true, length = 63)
    private String schemaName;

    // PENDING during creation, ACTIVE when ready, SUSPENDED when deactivated
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TenantStatus status;

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
