package com.techdesk.ticket.config;

import com.techdesk.ticket.multitenancy.TenantConnectionProvider;
import com.techdesk.ticket.multitenancy.TenantIsolationGuard;
import com.techdesk.ticket.multitenancy.TenantSchemaResolver;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Wires the three Hibernate multi-tenancy SPI components into the JPA EntityManagerFactory
@Configuration
public class HibernateMultiTenancyConfig {

    private final TenantSchemaResolver resolver;
    private final TenantConnectionProvider connectionProvider;
    private final TenantIsolationGuard isolationGuard;

    public HibernateMultiTenancyConfig(TenantSchemaResolver resolver,
                                       TenantConnectionProvider connectionProvider,
                                       TenantIsolationGuard isolationGuard) {
        this.resolver = resolver;
        this.connectionProvider = connectionProvider;
        this.isolationGuard = isolationGuard;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernateMultiTenancyCustomizer() {
        return props -> {
            props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
            // StatementInspector key — Hibernate 6 property name
            props.put("hibernate.session_factory.statement_inspector", isolationGuard);
        };
    }
}
