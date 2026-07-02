package com.techdesk.ticket.multitenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unit-level tests for TenantContext and TenantIsolationGuard — no Spring context needed
class TenantIsolationTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("TenantContext stores and retrieves tenant info correctly")
    void contextStoresInfo() {
        TenantContext.set(new TenantContext.TenantInfo("tenant_ats", "tenant_ats"));
        assertThat(TenantContext.get().schemaName()).isEqualTo("tenant_ats");
    }

    @Test
    @DisplayName("getRequired() throws when context is empty")
    void getRequiredThrowsWhenEmpty() {
        assertThatThrownBy(TenantContext::getRequired)
                .isInstanceOf(TenantIsolationException.class);
    }

    @Test
    @DisplayName("TenantContext is cleared after clear()")
    void clearResetsContext() {
        TenantContext.set(new TenantContext.TenantInfo("tenant_ats", "tenant_ats"));
        TenantContext.clear();
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    @DisplayName("IsolationGuard allows SQL when schema reference matches active tenant")
    void guardAllowsMatchingSchema() {
        TenantContext.set(new TenantContext.TenantInfo("tenant_ats", "tenant_ats"));
        TenantIsolationGuard guard = new TenantIsolationGuard();
        String sql = "select * from tenant_ats.tickets where id = ?";
        assertThat(guard.inspect(sql)).isEqualTo(sql);
    }

    @Test
    @DisplayName("IsolationGuard blocks SQL when schema reference mismatches active tenant")
    void guardBlocksCrossTenantSQL() {
        TenantContext.set(new TenantContext.TenantInfo("tenant_ats", "tenant_ats"));
        TenantIsolationGuard guard = new TenantIsolationGuard();
        String sql = "select * from tenant_acme.tickets where id = ?";

        assertThatThrownBy(() -> guard.inspect(sql))
                .isInstanceOf(TenantIsolationException.class)
                .hasMessageContaining("tenant_ats")
                .hasMessageContaining("tenant_acme");
    }

    @Test
    @DisplayName("IsolationGuard passes public schema references — needed for global table joins")
    void guardAllowsPublicSchema() {
        TenantContext.set(new TenantContext.TenantInfo("tenant_ats", "tenant_ats"));
        TenantIsolationGuard guard = new TenantIsolationGuard();
        // public.tenants join is legitimate and must not be blocked
        String sql = "select t.id from public.tenants t where t.schema_name = ?";
        assertThat(guard.inspect(sql)).isEqualTo(sql);
    }

    @Test
    @DisplayName("Concurrent threads carry isolated TenantContext — no cross-contamination")
    void concurrentThreadsAreIsolated() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<String> atsTask = () -> {
            TenantContext.set(new TenantContext.TenantInfo("tenant_ats", "tenant_ats"));
            // Simulate some processing time to force overlap with the other thread
            Thread.sleep(50);
            return TenantContext.get().schemaName();
        };

        Callable<String> acmeTask = () -> {
            TenantContext.set(new TenantContext.TenantInfo("tenant_acme", "tenant_acme"));
            Thread.sleep(50);
            return TenantContext.get().schemaName();
        };

        List<Future<String>> results = pool.invokeAll(List.of(atsTask, acmeTask));
        pool.shutdown();

        List<String> schemas = new ArrayList<>();
        for (Future<String> f : results) {
            schemas.add(f.get());
        }

        assertThat(schemas).containsExactlyInAnyOrder("tenant_ats", "tenant_acme");
    }
}
