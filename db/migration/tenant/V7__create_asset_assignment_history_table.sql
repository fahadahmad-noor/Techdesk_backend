-- Create asset assignment history table
CREATE TABLE asset_assignment_history (
    id UUID PRIMARY KEY,
    asset_id UUID REFERENCES assets(id) ON DELETE CASCADE,
    employee_id UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    returned_at TIMESTAMP
);

CREATE INDEX idx_asset_hist_asset ON asset_assignment_history(asset_id);
CREATE INDEX idx_asset_hist_emp ON asset_assignment_history(employee_id);
