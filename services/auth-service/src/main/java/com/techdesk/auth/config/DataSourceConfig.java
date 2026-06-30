package com.techdesk.auth.config;

import com.techdesk.auth.multitenancy.TenantAwareDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;

/**
 * Wraps the standard Spring Boot DataSource with our TenantAwareDataSource
 * so that every JDBC connection automatically applies the correct schema search_path.
 *
 * TODO Phase 3.4: Remove this config and replace with Hibernate multi-tenancy configuration.
 */
@Configuration
public class DataSourceConfig {

    /**
     * The raw DataSource configured from application.yml properties.
     * Kept as a separate bean so TenantAwareDataSource can delegate to it.
     */
    @Bean(name = "rawDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource rawDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * The primary DataSource used by all JPA and JDBC operations.
     * Wraps the raw DataSource with tenant schema routing.
     */
    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource(@Qualifier("rawDataSource") DataSource rawDataSource) {
        return new TenantAwareDataSource(rawDataSource);
    }
}
