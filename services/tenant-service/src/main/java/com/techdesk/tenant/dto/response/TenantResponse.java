package com.techdesk.tenant.dto.response;

import com.techdesk.tenant.entity.TenantPlan;
import com.techdesk.tenant.entity.TenantStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outgoing payload returned after creating a tenant or fetching tenant details.
 * Exposed to the Super Admin via the tenant management endpoints.
 */
public class TenantResponse {

    private UUID id;
    private String companyName;
    private String schemaName;
    private TenantStatus status;
    private TenantPlan plan;
    private String adminEmail;
    private LocalDateTime createdAt;

    public TenantResponse() {}

    public TenantResponse(UUID id, String companyName, String schemaName,
                          TenantStatus status, TenantPlan plan,
                          String adminEmail, LocalDateTime createdAt) {
        this.id = id;
        this.companyName = companyName;
        this.schemaName = schemaName;
        this.status = status;
        this.plan = plan;
        this.adminEmail = adminEmail;
        this.createdAt = createdAt;
    }

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }

    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }

    public TenantPlan getPlan() { return plan; }
    public void setPlan(TenantPlan plan) { this.plan = plan; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
