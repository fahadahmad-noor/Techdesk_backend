package com.techdesk.auth.repository;

import com.techdesk.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User — queries the tenant-schema users table.
 * The schema is resolved at runtime by TenantAwareDataSource.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email within the current tenant's schema.
     */
    Optional<User> findByEmail(String email);
}
