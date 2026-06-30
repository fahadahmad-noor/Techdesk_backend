package com.techdesk.tenant.exception;

// Thrown when CREATE SCHEMA or Flyway migration fails — SchemaProvisioningService guarantees full rollback
public class SchemaProvisioningException extends RuntimeException {

    public SchemaProvisioningException(String schemaName, Throwable cause) {
        super("Failed to provision database schema '" + schemaName + "'. All changes have been rolled back.", cause);
    }
}
