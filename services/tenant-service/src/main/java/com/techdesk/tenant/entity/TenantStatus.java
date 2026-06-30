package com.techdesk.tenant.entity;

/**
 * Represents the lifecycle state of a tenant account on the TechDesk platform.
 */
public enum TenantStatus {
    /** Schema provisioning is in progress. No users can log in yet. */
    PENDING,
    /** The tenant is fully operational. All users can access the platform. */
    ACTIVE,
    /** The tenant account has been deactivated. Users cannot log in. */
    SUSPENDED
}
