package com.techdesk.tenant.entity;

// Tracks where a tenant is in its lifecycle — PENDING during setup, ACTIVE when live, SUSPENDED when deactivated
public enum TenantStatus {
    PENDING,
    ACTIVE,
    SUSPENDED
}
