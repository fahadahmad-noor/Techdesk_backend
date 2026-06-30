package com.techdesk.tenant.service;

import com.techdesk.tenant.exception.SchemaProvisioningException;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

// Creates the tenant PostgreSQL schema and runs Flyway migrations — drops schema on any failure
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

    // Runs CREATE SCHEMA then Flyway — on any error, rolls back with DROP SCHEMA CASCADE
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

    private void createSchema(String schemaName) {
        log.debug("Executing CREATE SCHEMA for: {}", schemaName);
        jdbcTemplate.execute("CREATE SCHEMA \"" + schemaName + "\"");
        log.info("Schema created: {}", schemaName);
    }

    // Builds a Flyway instance pointing at the new schema and runs all V1-V11 migration scripts
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

    // Best-effort cleanup — errors here are suppressed so the original exception propagates
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
