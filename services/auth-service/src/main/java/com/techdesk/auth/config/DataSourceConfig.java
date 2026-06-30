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

// Wraps the default DataSource with tenant schema routing — will be replaced by Hibernate multi-tenancy in Phase 3.4
@Configuration
public class DataSourceConfig {

    // Raw datasource from application.yml — used as the delegate inside TenantAwareDataSource
    @Bean(name = "rawDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource rawDataSource() {
        return DataSourceBuilder.create().build();
    }

    // Primary datasource — routes every connection to the correct tenant schema automatically
    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource(@Qualifier("rawDataSource") DataSource rawDataSource) {
        return new TenantAwareDataSource(rawDataSource);
    }
}
