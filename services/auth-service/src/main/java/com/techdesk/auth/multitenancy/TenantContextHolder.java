package com.techdesk.auth.multitenancy;

/**
 * ThreadLocal holder for the current tenant's PostgreSQL schema name.
 * Set at the start of each request by {@link TenantInterceptor}.
 * Cleared at the end of each request to prevent memory leaks.
 *
 * TODO Phase 3.4: Replace with Hibernate CurrentTenantIdentifierResolver.
 */
public class TenantContextHolder {

    private static final ThreadLocal<String> currentSchema = new ThreadLocal<>();

    private TenantContextHolder() {
        // Utility class — not instantiable
    }

    /**
     * Sets the schema name for the current thread.
     * @param schemaName PostgreSQL schema name (e.g. "tenant_companya")
     */
    public static void setCurrentSchema(String schemaName) {
        currentSchema.set(schemaName);
    }

    /**
     * Returns the schema name for the current thread.
     * @return schema name, or null if not set
     */
    public static String getCurrentSchema() {
        return currentSchema.get();
    }

    /**
     * Clears the schema name for the current thread.
     * Must be called in the afterCompletion of the interceptor to prevent leaks.
     */
    public static void clear() {
        currentSchema.remove();
    }
}
