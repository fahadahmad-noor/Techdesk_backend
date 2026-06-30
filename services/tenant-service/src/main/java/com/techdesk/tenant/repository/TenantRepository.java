package com.techdesk.tenant.repository;

import com.techdesk.tenant.entity.Tenant;
import com.techdesk.tenant.entity.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// DAO for public.tenants — never touches a tenant schema
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsByName(String name);

    boolean existsBySchemaName(String schemaName);

    Page<Tenant> findByStatus(TenantStatus status, Pageable pageable);

    Optional<Tenant> findBySchemaName(String schemaName);
}
