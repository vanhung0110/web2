-- V9__create_loyalty_and_maintenance.sql

CREATE TABLE loyalty_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_id UUID NOT NULL UNIQUE REFERENCES guests(id),
    total_points BIGINT NOT NULL DEFAULT 0,
    available_points BIGINT NOT NULL DEFAULT 0,
    used_points BIGINT NOT NULL DEFAULT 0,
    tier VARCHAR(20) NOT NULL DEFAULT 'BRONZE',
    tier_upgraded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_loyalty_guest ON loyalty_accounts(guest_id);

CREATE TABLE maintenance_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID REFERENCES rooms(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    reported_by_id UUID REFERENCES staff(id),
    assigned_to_id UUID REFERENCES staff(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
    reported_at TIMESTAMP DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    repair_cost NUMERIC(15,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE maintenance_before_photos (
    request_id UUID NOT NULL REFERENCES maintenance_requests(id) ON DELETE CASCADE,
    photo_key VARCHAR(500) NOT NULL
);

CREATE TABLE maintenance_after_photos (
    request_id UUID NOT NULL REFERENCES maintenance_requests(id) ON DELETE CASCADE,
    photo_key VARCHAR(500) NOT NULL
);

CREATE INDEX idx_maintenance_property ON maintenance_requests(property_id);
CREATE INDEX idx_maintenance_room ON maintenance_requests(room_id);
CREATE INDEX idx_maintenance_status ON maintenance_requests(status);
CREATE INDEX idx_maintenance_assigned ON maintenance_requests(assigned_to_id);
