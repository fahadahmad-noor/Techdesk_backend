-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'COMPANY_ADMIN', 'IT_MANAGER', 'IT_STAFF', 'EMPLOYEE', 'AUDITOR')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'INVITED')),
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
