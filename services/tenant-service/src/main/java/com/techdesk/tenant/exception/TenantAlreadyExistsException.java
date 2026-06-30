package com.techdesk.tenant.exception;

/**
 * Thrown when a request attempts to create a tenant with a company name
 * that already exists in the public.tenants table.
 * Maps to HTTP 409 Conflict via GlobalExceptionHandler.
 */
public class TenantAlreadyExistsException extends RuntimeException {

    public TenantAlreadyExistsException(String companyName) {
        super("A company with the name '" + companyName + "' already exists on this platform.");
    }
}
