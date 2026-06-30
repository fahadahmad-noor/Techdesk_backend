package com.techdesk.tenant.exception;

// Maps to HTTP 409 Conflict — raised on duplicate company name or schema name
public class TenantAlreadyExistsException extends RuntimeException {

    public TenantAlreadyExistsException(String companyName) {
        super("A company with the name '" + companyName + "' already exists on this platform.");
    }
}
