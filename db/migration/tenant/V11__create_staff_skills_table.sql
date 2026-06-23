-- Create staff skills table
CREATE TABLE staff_skills (
    staff_id UUID REFERENCES users(id) ON DELETE CASCADE,
    skill VARCHAR(50) NOT NULL,
    PRIMARY KEY (staff_id, skill)
);
