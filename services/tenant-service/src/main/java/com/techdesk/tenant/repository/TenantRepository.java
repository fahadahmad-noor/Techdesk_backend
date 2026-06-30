package com.techdesk.tenant.repository;

import com.techdesk.tenant.entity.Tenant;
import com.techdesk.tenant.entity.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for the public.tenants table.
 * All queries are scoped to the public schema — this repository never touches a tenant schema.
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /** Checks for an existing tenant with the same company name to prevent duplicates. */
    boolean existsByName(String name);

    /** Used during schema name generation to verify uniqueness before provisioning. */
    boolean existsBySchemaName(String schemaName);

    /** Supports the GET /api/tenants endpoint with optional status filtering and pagination. */
    Page<Tenant> findByStatus(TenantStatus status, Pageable pageable);

    /** Looks up a tenant by its schema name — used by auth-service JWT validation. */
    Optional<Tenant> findBySchemaName(String schemaName);
}
