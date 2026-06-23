-- Create public audit logs table (immutable event log)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50),
    actor_id UUID,
    actor_email VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    old_value JSONB,
    new_value JSONB,
    signature VARCHAR(64), -- HMAC signature for tampering check
    ip_address VARCHAR(45),
    user_agent TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
