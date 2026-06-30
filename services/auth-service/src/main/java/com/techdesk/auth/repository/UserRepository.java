package com.techdesk.auth.repository;

import com.techdesk.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Queries the tenant schema users table — schema routing is handled by TenantAwareDataSource
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
}
