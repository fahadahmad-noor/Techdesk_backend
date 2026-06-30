package com.techdesk.auth.multitenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

// Sets PostgreSQL search_path on every connection based on TenantContextHolder — Phase 3.4 will replace this
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

    // Sets search_path to "tenant_schema, public" so tenant and shared tables are both reachable
    private void applyTenantSchema(Connection connection) throws SQLException {
        String schema = TenantContextHolder.getCurrentSchema();
        String searchPath = (schema != null) ? schema + ", public" : "public";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO " + searchPath);
            log.debug("Set search_path to: {}", searchPath);
        }
    }
}
