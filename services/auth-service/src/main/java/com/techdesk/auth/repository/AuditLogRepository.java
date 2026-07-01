package com.techdesk.auth.repository;

import com.techdesk.auth.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Temporary direct write to audit_logs — will be replaced by ApplicationEvent publishing in Phase 6.3
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
