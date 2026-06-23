-- Create gadget requests table
CREATE TABLE gadget_requests (
    id UUID PRIMARY KEY,
    employee_id UUID REFERENCES users(id) ON DELETE CASCADE,
    gadget_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('REQUESTED', 'PENDING_MANAGER_APPROVAL', 'APPROVED', 'REJECTED', 'IN_PROCUREMENT', 'READY_FOR_PICKUP', 'ASSIGNED', 'CANCELLED')),
    reason TEXT NOT NULL,
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    rejection_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gadget_req_emp ON gadget_requests(employee_id);
CREATE INDEX idx_gadget_req_status ON gadget_requests(status);
