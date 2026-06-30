package com.techdesk.auth.repository;

import com.techdesk.auth.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for public.tenants table.
 * Used by TenantInterceptor to resolve the schema_name from X-Tenant-ID header.
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Finds a tenant by their URL-friendly name (e.g. "company-a").
     */
    Optional<Tenant> findByName(String name);
}
