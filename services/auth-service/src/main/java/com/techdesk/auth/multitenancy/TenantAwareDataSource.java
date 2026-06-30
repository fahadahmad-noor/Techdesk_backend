package com.techdesk.auth.multitenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A DataSource wrapper that sets the PostgreSQL search_path on every connection
 * based on the schema stored in {@link TenantContextHolder}.
 *
 * This is the Phase 3.2 simplified approach for tenant-aware DB access.
 * It always falls back to public schema if no tenant is set.
 *
 * TODO Phase 3.4: Replace this with Hibernate MultiTenantConnectionProvider.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareDataSource.class);

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        applyTenantSchema(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        applyTenantSchema(connection);
        return connection;
    }

    /**
     * Sets the PostgreSQL search_path for the connection.
     * If a tenant schema is present, it takes priority over public.
     * Listing "public" ensures public-schema tables (tenants, audit_logs) are also accessible.
     *
     * @param connection the JDBC connection to configure
     */
    private void applyTenantSchema(Connection connection) throws SQLException {
        String schema = TenantContextHolder.getCurrentSchema();
        String searchPath = (schema != null) ? schema + ", public" : "public";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO " + searchPath);
            log.debug("Set search_path to: {}", searchPath);
        }
    }
}
