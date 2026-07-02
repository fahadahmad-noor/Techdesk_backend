package com.techdesk.ticket.multitenancy;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

// Acquires a pooled connection and switches its search_path to the tenant schema before Hibernate uses it
@Component
public class TenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final Logger log = LoggerFactory.getLogger(TenantConnectionProvider.class);
    private static final Pattern SAFE_SCHEMA = Pattern.compile("^tenant_[a-z0-9_]+$");

    private final DataSource dataSource;

    public TenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        if (!SAFE_SCHEMA.matcher(tenantIdentifier).matches()) {
            throw new TenantIsolationException(tenantIdentifier, "INVALID_FORMAT");
        }
        Connection conn = dataSource.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO " + tenantIdentifier + ", public");
            log.debug("search_path switched to [{}, public]", tenantIdentifier);
        }
        return conn;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        // Reset to public before returning to pool — prevents schema bleed to next request
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO public");
        }
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnsupportedOperationException("Unwrap not supported");
    }
}
