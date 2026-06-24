package com.techdesk.auth.repository;

import com.techdesk.auth.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for public.audit_logs.
 * Used to write security events (e.g. replay attacks) directly until
 * the event-driven audit system is built in Phase 6.3.
 *
 * TODO Phase 6.3: Remove this repository from auth-service.
 *                 Security events will be published via ApplicationEvent instead.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
