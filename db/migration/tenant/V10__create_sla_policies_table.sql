-- Create SLA policies table
CREATE TABLE sla_policies (
    id UUID PRIMARY KEY,
    category_id UUID REFERENCES ticket_categories(id) ON DELETE CASCADE,
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    response_time_hours INT NOT NULL,
    resolution_time_hours INT NOT NULL,
    UNIQUE (category_id, priority)
);
