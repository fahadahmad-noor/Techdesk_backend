-- Create tenants table
CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    schema_name VARCHAR(63) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING')),
    plan VARCHAR(20) NOT NULL CHECK (plan IN ('BASIC', 'PREMIUM', 'ENTERPRISE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenants_schema ON tenants(schema_name);
