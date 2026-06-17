-- V6__create_staff_and_shifts.sql
-- Tạo bảng staff và shifts

CREATE TABLE staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE REFERENCES users(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    address VARCHAR(500),
    role VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date DATE,
    end_date DATE,
    avatar_key VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_staff_property ON staff(property_id);
CREATE INDEX idx_staff_user ON staff(user_id);
CREATE INDEX idx_staff_is_active ON staff(property_id, is_active);

CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id UUID NOT NULL REFERENCES staff(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    scheduled_start TIMESTAMP NOT NULL,
    scheduled_end TIMESTAMP NOT NULL,
    actual_start TIMESTAMP,
    actual_end TIMESTAMP,
    type VARCHAR(20) NOT NULL,
    notes TEXT,
    is_overtime BOOLEAN DEFAULT FALSE,
    clock_in_latitude DOUBLE PRECISION,
    clock_in_longitude DOUBLE PRECISION,
    clock_out_latitude DOUBLE PRECISION,
    clock_out_longitude DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_shifts_staff ON shifts(staff_id);
CREATE INDEX idx_shifts_property_date ON shifts(property_id, scheduled_start);
