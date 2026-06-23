-- Create assets table
CREATE TABLE assets (
    id UUID PRIMARY KEY,
    asset_tag VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('LAPTOP', 'MONITOR', 'KEYBOARD', 'MOUSE', 'HEADPHONES', 'PRINTER', 'ROUTER', 'OTHER')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'UNDER_REPAIR', 'IN_PROCUREMENT', 'LOST', 'DAMAGED', 'RETIRED')),
    assigned_to UUID REFERENCES users(id) ON DELETE SET NULL,
    purchase_date DATE,
    warranty_expiry DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_assets_tag ON assets(asset_tag);
CREATE INDEX idx_assets_status ON assets(status);
