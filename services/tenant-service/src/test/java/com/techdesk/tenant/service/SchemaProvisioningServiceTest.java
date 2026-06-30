package com.techdesk.tenant.service;

import com.techdesk.tenant.exception.SchemaProvisioningException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SchemaProvisioningService.
 *
 * Validates the critical rollback guarantee: if schema creation succeeds but Flyway
 * migration fails, the schema must be dropped before the exception propagates.
 *
 * Note: Because Flyway is constructed internally (not injected), these tests verify
 * rollback behavior by making CREATE SCHEMA succeed and then simulating a downstream failure.
 */
@ExtendWith(MockitoExtension.class)
class SchemaProvisioningServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Test
    @DisplayName("provisionSchema — drops schema and throws when an unexpected error occurs after schema creation")
    void provisionSchema_failureAfterCreation_dropsSchemaAndThrows() {
        SchemaProvisioningService service = new SchemaProvisioningService(
                jdbcTemplate, dataSource, "classpath:db/migration/tenant");

        // Simulate CREATE SCHEMA succeeding, then an immediate failure in the same call chain.
        // We simulate this by making the second JdbcTemplate execute call throw.
        doNothing()
                .doThrow(new RuntimeException("Simulated failure after schema creation"))
                .when(jdbcTemplate).execute(anyString());

        SchemaProvisioningException ex = assertThrows(SchemaProvisioningException.class,
                () -> service.provisionSchema("tenant_test_rollback"));

        assertTrue(ex.getMessage().contains("tenant_test_rollback"));

        // Verify that DROP SCHEMA was called as part of the rollback.
        verify(jdbcTemplate, atLeastOnce()).execute(contains("DROP SCHEMA"));
    }

    @Test
    @DisplayName("provisionSchema — does NOT call DROP SCHEMA when CREATE SCHEMA itself fails (nothing was created to clean up)")
    void provisionSchema_schemaCreationFails_doesNotAttemptRollback() {
        SchemaProvisioningService service = new SchemaProvisioningService(
                jdbcTemplate, dataSource, "classpath:db/migration/tenant");

        doThrow(new RuntimeException("CREATE SCHEMA failed — permission denied"))
                .when(jdbcTemplate).execute(contains("CREATE SCHEMA"));

        assertThrows(SchemaProvisioningException.class,
                () -> service.provisionSchema("tenant_test_create_fail"));

        // When CREATE SCHEMA fails, there is no schema to clean up.
        // Verify that DROP SCHEMA is never called — rollback of nothing is wasteful and incorrect.
        verify(jdbcTemplate, never()).execute(contains("DROP SCHEMA"));
    }
}
