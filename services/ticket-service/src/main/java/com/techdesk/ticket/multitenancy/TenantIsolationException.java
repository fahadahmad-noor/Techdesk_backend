package com.techdesk.ticket.multitenancy;

// Thrown by TenantIsolationGuard when a SQL statement targets an unexpected schema
public class TenantIsolationException extends RuntimeException {

    private final String expectedSchema;
    private final String detectedSchema;

    public TenantIsolationException(String expectedSchema, String detectedSchema) {
        super("Tenant isolation breach: expected [" + expectedSchema + "] but SQL targeted [" + detectedSchema + "]");
        this.expectedSchema = expectedSchema;
        this.detectedSchema = detectedSchema;
    }

    public String getExpectedSchema() { return expectedSchema; }
    public String getDetectedSchema() { return detectedSchema; }
}
