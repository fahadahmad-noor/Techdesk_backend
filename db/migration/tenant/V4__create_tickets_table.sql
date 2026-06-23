-- Create tickets table with optimistic lock version
CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('OPEN', 'AUTO_ASSIGNED', 'IN_PROGRESS', 'WAITING_FOR_EMPLOYEE', 'WAITING_FOR_APPROVAL', 'ESCALATED', 'RESOLVED', 'CLOSED', 'REOPENED', 'CANCELLED')),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    category_id UUID REFERENCES ticket_categories(id),
    reporter_id UUID REFERENCES users(id),
    assigned_to UUID REFERENCES users(id),
    sla_due_at TIMESTAMP,
    is_sla_breached BOOLEAN DEFAULT FALSE,
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_priority ON tickets(priority);
CREATE INDEX idx_tickets_reporter ON tickets(reporter_id);
CREATE INDEX idx_tickets_assigned ON tickets(assigned_to);
