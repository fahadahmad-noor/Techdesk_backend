package com.techdesk.auth.multitenancy;

// Stores the current tenant's schema name per thread — set by TenantInterceptor, cleared after each request
// TODO Phase 3.4: Replace with Hibernate CurrentTenantIdentifierResolver
public class TenantContextHolder {

    private static final ThreadLocal<String> currentSchema = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void setCurrentSchema(String schemaName) {
        currentSchema.set(schemaName);
    }

    public static String getCurrentSchema() {
        return currentSchema.get();
    }

    // Must be called at end of every request to avoid ThreadLocal leaks
    public static void clear() {
        currentSchema.remove();
    }
}
