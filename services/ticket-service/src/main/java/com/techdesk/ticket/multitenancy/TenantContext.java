package com.techdesk.ticket.multitenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Holds the active tenant's schema for the current request thread — must be cleared after every request
public final class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);

    // InheritableThreadLocal so async tasks spawned from a request thread inherit the tenant
    private static final InheritableThreadLocal<TenantInfo> holder = new InheritableThreadLocal<>();

    public record TenantInfo(String tenantId, String schemaName) {}

    private TenantContext() {}

    public static void set(TenantInfo info) {
        holder.set(info);
        log.debug("TenantContext set — tenantId={} schema={}", info.tenantId(), info.schemaName());
    }

    public static TenantInfo get() {
        return holder.get();
    }

    // Use this in any path where a missing tenant is a programming error, not a user error
    public static TenantInfo getRequired() {
        TenantInfo info = holder.get();
        if (info == null) {
            throw new TenantIsolationException("UNSET", "null");
        }
        return info;
    }

    public static void clear() {
        holder.remove();
    }
}
