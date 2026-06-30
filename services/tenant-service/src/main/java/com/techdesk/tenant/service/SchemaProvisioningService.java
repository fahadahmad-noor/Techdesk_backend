package com.techdesk.tenant.service;

import com.techdesk.tenant.exception.SchemaProvisioningException;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * Responsible for the low-level database operations required when a new tenant is onboarded.
 *
 * This service handles two operations in strict sequence:
 *   1. CREATE SCHEMA — allocates an isolated PostgreSQL schema for the new tenant.
 *   2. Flyway migration — runs all tenant-specific SQL scripts against the new schema,
 *      creating the full table structure (users, tickets, assets, etc.).
 *
 * Rollback guarantee: if either step fails, the schema is dropped via DROP SCHEMA ... CASCADE
 * before the exception is re-thrown, ensuring the database is never left in a partial state.
 */
@Service
public class SchemaProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(SchemaProvisioningService.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final String tenantMigrationLocation;

    public SchemaProvisioningService(JdbcTemplate jdbcTemplate,
                                     DataSource dataSource,
                                     @Value("${app.tenant-migration-location}") String tenantMigrationLocation) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.tenantMigrationLocation = tenantMigrationLocation;
    }

    /**
     * Creates the PostgreSQL schema for a new tenant and runs all Flyway tenant migrations against it.
     *
     * If schema creation succeeds but the Flyway migration fails, the schema is dropped completely
     * (DROP SCHEMA ... CASCADE) before throwing SchemaProvisioningException, guaranteeing a clean rollback.
     *
     * @param schemaName the validated schema name for the new tenant (e.g., "tenant_acme_corp")
     * @throws SchemaProvisioningException if either schema creation or migration fails
     */
    public void provisionSchema(String schemaName) {
        log.info("Starting schema provisioning for: {}", schemaName);

        try {
            createSchema(schemaName);
            runMigrations(schemaName);
            log.info("Schema provisioning completed successfully for: {}", schemaName);

        } catch (Exception ex) {
            log.error("Schema provisioning failed for: {}. Initiating rollback.", schemaName, ex);
            attemptRollback(schemaName);
            throw new SchemaProvisioningException(schemaName, ex);
        }
    }

    /**
     * Executes CREATE SCHEMA against the PostgreSQL database.
     * Uses quoted identifiers to safely handle schema names with underscores.
     */
    private void createSchema(String schemaName) {
        log.debug("Executing CREATE SCHEMA for: {}", schemaName);
        jdbcTemplate.execute("CREATE SCHEMA \"" + schemaName + "\"");
        log.info("Schema created: {}", schemaName);
    }

    /**
     * Builds a programmatic Flyway instance targeting the new tenant schema and runs all
     * migration scripts from the configured tenant migration classpath location.
     *
     * Each tenant gets the same set of tables (V1 through V11) created in their isolated schema.
     */
    private void runMigrations(String schemaName) {
        log.debug("Running Flyway migrations against schema: {}", schemaName);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations(tenantMigrationLocation)
                .baselineOnMigrate(false)
                .load();

        flyway.migrate();
        log.info("Flyway migrations completed for schema: {}", schemaName);
    }

    /**
     * Attempts to drop the newly created schema and all its contents.
     * Called only when provisioning fails, to restore the database to its pre-creation state.
     * Errors during rollback are logged but suppressed to allow the original exception to propagate.
     */
    private void attemptRollback(String schemaName) {
        try {
            log.warn("Rolling back: dropping schema {}", schemaName);
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            log.info("Rollback successful: schema {} has been dropped.", schemaName);
        } catch (Exception rollbackEx) {
            log.error("CRITICAL: Rollback failed for schema {}. Manual cleanup may be required.", schemaName, rollbackEx);
        }
    }
}
