package com.techdesk.tenant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Request DTO for POST /api/tenants — submitted by Super Admin when onboarding a new company
public class CreateTenantRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;

    @NotBlank(message = "Plan is required")
    @Pattern(regexp = "BASIC|PREMIUM|ENTERPRISE", message = "Plan must be BASIC, PREMIUM, or ENTERPRISE")
    private String plan;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be a valid email address")
    private String adminEmail;

    @NotBlank(message = "Admin first name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String adminLastName;

    // Getters and Setters

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }

    public String getAdminFirstName() { return adminFirstName; }
    public void setAdminFirstName(String adminFirstName) { this.adminFirstName = adminFirstName; }

    public String getAdminLastName() { return adminLastName; }
    public void setAdminLastName(String adminLastName) { this.adminLastName = adminLastName; }
}
