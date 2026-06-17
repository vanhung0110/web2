-- V2__create_rooms_and_room_types.sql
-- Tạo bảng room_types và rooms

CREATE TABLE room_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties(id),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description TEXT,
    base_price NUMERIC(15,2) NOT NULL,
    max_occupancy INTEGER,
    bed_count INTEGER,
    bed_type VARCHAR(20),
    area DOUBLE PRECISION,
    amenities TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    total_rooms INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_room_types_property ON room_types(property_id);

CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties(id),
    room_type_id UUID NOT NULL REFERENCES room_types(id),
    room_number VARCHAR(20) NOT NULL,
    floor INTEGER NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    initial_water_index DOUBLE PRECISION DEFAULT 0,
    initial_electric_index DOUBLE PRECISION DEFAULT 0,
    has_balcony BOOLEAN DEFAULT FALSE,
    has_window BOOLEAN DEFAULT TRUE,
    view_type VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_rooms_property ON rooms(property_id);
CREATE INDEX idx_rooms_room_type ON rooms(room_type_id);
CREATE INDEX idx_rooms_status ON rooms(status);
CREATE UNIQUE INDEX idx_rooms_property_number ON rooms(property_id, room_number) WHERE deleted = FALSE;

CREATE TABLE room_image_keys (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    image_key VARCHAR(500) NOT NULL
);
