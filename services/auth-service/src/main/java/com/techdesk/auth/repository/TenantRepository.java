package com.techdesk.auth.repository;

import com.techdesk.auth.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Read-only DAO for public.tenants — used by TenantInterceptor to look up schema from X-Tenant-ID header
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByName(String name);
}
