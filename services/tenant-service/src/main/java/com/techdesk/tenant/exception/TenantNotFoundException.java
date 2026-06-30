package com.techdesk.tenant.exception;

/**
 * Thrown when a tenant with the given ID cannot be found in the public.tenants table.
 * Maps to HTTP 404 Not Found via GlobalExceptionHandler.
 */
public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String id) {
        super("No tenant found with ID: " + id);
    }
}
