package com.techdesk.ticket.multitenancy;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Last line of defence — inspects every SQL string before it hits the DB and blocks cross-tenant queries
@Component
public class TenantIsolationGuard implements StatementInspector {

    private static final Logger log = LoggerFactory.getLogger(TenantIsolationGuard.class);

    // Matches any schema reference like "tenant_ats." in raw SQL
    private static final Pattern SCHEMA_REF = Pattern.compile("\\b(tenant_[a-z0-9_]+)\\.");

    @Override
    public String inspect(String sql) {
        TenantContext.TenantInfo info = TenantContext.get();
        if (info == null) {
            // System/admin queries with no tenant context pass through unchecked
            return sql;
        }

        Matcher m = SCHEMA_REF.matcher(sql);
        while (m.find()) {
            String found = m.group(1);
            if (!found.equals(info.schemaName())) {
                log.error("ISOLATION BREACH — expected schema [{}] but SQL references [{}] | sql={}",
                        info.schemaName(), found, sql);
                throw new TenantIsolationException(info.schemaName(), found);
            }
        }

        return sql;
    }
}
