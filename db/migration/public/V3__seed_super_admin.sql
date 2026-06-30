-- ============================================================
-- V3: Seed the default SUPER_ADMIN user
--
-- This is the platform owner account used by Adept Tech Solutions
-- to manage all tenants on the TechDesk platform.
--
-- The SUPER_ADMIN does NOT belong to any tenant schema.
-- They exist in the public schema's super_admins table.
--
-- Password: SuperAdmin123!
-- BCrypt hash (cost factor 12) — change this on first login in production.
-- ============================================================

CREATE TABLE IF NOT EXISTS super_admins (
    id            UUID PRIMARY KEY,
    email         VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(50)  NOT NULL,
    last_name     VARCHAR(50)  NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'SUPER_ADMIN',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert the default Super Admin.
-- IMPORTANT: Change the password immediately after first deployment.
-- Default credentials: email = superadmin@techdesk.io | password = SuperAdmin123!
INSERT INTO super_admins (id, email, password_hash, first_name, last_name)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'superadmin@techdesk.io',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
    'Super',
    'Admin'
)
ON CONFLICT (email) DO NOTHING;
