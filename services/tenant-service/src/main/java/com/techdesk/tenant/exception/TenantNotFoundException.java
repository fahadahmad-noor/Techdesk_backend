package com.techdesk.tenant.exception;

// Maps to HTTP 404 Not Found
public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String id) {
        super("No tenant found with ID: " + id);
    }
}
