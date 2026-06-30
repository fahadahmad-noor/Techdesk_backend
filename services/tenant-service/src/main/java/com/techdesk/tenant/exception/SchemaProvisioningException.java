package com.techdesk.tenant.exception;

/**
 * Thrown when the programmatic schema provisioning process fails during tenant creation.
 * This includes failures in CREATE SCHEMA execution or Flyway migration runs.
 *
 * When this exception is thrown, the SchemaProvisioningService guarantees that all
 * database changes (schema creation, tenant record insertion) have been fully rolled back.
 *
 * Maps to HTTP 500 Internal Server Error via GlobalExceptionHandler.
 */
public class SchemaProvisioningException extends RuntimeException {

    public SchemaProvisioningException(String schemaName, Throwable cause) {
        super("Failed to provision database schema '" + schemaName + "'. All changes have been rolled back.", cause);
    }
}
