package com.techdesk.tenant.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler for the tenant-service.
 *
 * Catches all custom and framework exceptions and converts them into consistent,
 * structured JSON error responses. Controllers remain completely free of error-handling logic.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles duplicate company name attempts. Returns 409 Conflict.
     */
    @ExceptionHandler(TenantAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleTenantAlreadyExists(TenantAlreadyExistsException ex) {
        log.warn("Tenant creation conflict: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles requests for a non-existent tenant. Returns 404 Not Found.
     */
    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTenantNotFound(TenantNotFoundException ex) {
        log.warn("Tenant not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles schema provisioning or Flyway migration failures during tenant creation.
     * Returns 500 Internal Server Error. The operation will have been fully rolled back.
     */
    @ExceptionHandler(SchemaProvisioningException.class)
    public ResponseEntity<Map<String, Object>> handleSchemaProvisioning(SchemaProvisioningException ex) {
        log.error("Schema provisioning failed and was rolled back: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Tenant creation failed during database provisioning. The operation has been fully rolled back.");
    }

    /**
     * Handles DB unique constraint violations not already caught by domain-level guards.
     * This is a safety net for concurrent requests that pass the existsByName check
     * simultaneously — the second DB insert fails with a constraint violation.
     * Returns 409 Conflict.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT,
                "A resource with this identifier already exists.");
    }

    /**
     * Handles @Valid annotation failures on incoming request DTOs.
     * Returns 400 Bad Request with a field-level breakdown of validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Catch-all handler for any unhandled runtime exceptions. Returns 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception in tenant-service", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
