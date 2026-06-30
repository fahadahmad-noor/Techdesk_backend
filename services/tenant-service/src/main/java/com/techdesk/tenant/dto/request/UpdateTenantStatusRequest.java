package com.techdesk.tenant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Incoming payload for PATCH /api/tenants/{id}/status.
 * Submitted by the Super Admin to suspend or reactivate a company account.
 */
public class UpdateTenantStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|SUSPENDED", message = "Status must be ACTIVE or SUSPENDED")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
