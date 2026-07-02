package com.techdesk.ticket.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Hibernate SPI — called on every session open to determine which schema this session should target
@Component
public class TenantSchemaResolver implements CurrentTenantIdentifierResolver<String> {

    private static final Logger log = LoggerFactory.getLogger(TenantSchemaResolver.class);
    private static final String FALLBACK = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        TenantContext.TenantInfo info = TenantContext.get();
        if (info == null) {
            log.warn("TenantContext is empty — falling back to public schema");
            return FALLBACK;
        }
        return info.schemaName();
    }

    // Prevents Hibernate from reusing a stale session if tenant switches mid-thread (safety net)
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
