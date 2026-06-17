-- V4__create_utility_readings.sql
-- Tạo bảng utility_readings và utility_price_configs

CREATE TABLE utility_readings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id),
    room_id UUID NOT NULL REFERENCES rooms(id),

    -- Đồng hồ nước
    water_index_start DOUBLE PRECISION,
    water_index_end DOUBLE PRECISION,
    water_usage DOUBLE PRECISION,
    water_price_per_unit NUMERIC(15,2),
    water_total NUMERIC(15,2),
    water_photo_start_key VARCHAR(500),
    water_photo_end_key VARCHAR(500),
    water_photo_start_hash VARCHAR(64),
    water_photo_end_hash VARCHAR(64),
    water_photo_start_verified BOOLEAN DEFAULT FALSE,
    water_photo_end_verified BOOLEAN DEFAULT FALSE,
    water_manual_input_start DOUBLE PRECISION,
    water_manual_input_end DOUBLE PRECISION,
    water_manual_verified BOOLEAN DEFAULT FALSE,
    water_discrepancy_note TEXT,

    -- Đồng hồ điện
    electric_index_start DOUBLE PRECISION,
    electric_index_end DOUBLE PRECISION,
    electric_usage DOUBLE PRECISION,
    electric_price_per_unit NUMERIC(15,2),
    electric_total NUMERIC(15,2),
    electric_photo_start_key VARCHAR(500),
    electric_photo_end_key VARCHAR(500),
    electric_photo_start_hash VARCHAR(64),
    electric_photo_end_hash VARCHAR(64),
    electric_photo_start_verified BOOLEAN DEFAULT FALSE,
    electric_photo_end_verified BOOLEAN DEFAULT FALSE,
    electric_manual_input_start DOUBLE PRECISION,
    electric_manual_input_end DOUBLE PRECISION,
    electric_manual_verified BOOLEAN DEFAULT FALSE,
    electric_discrepancy_note TEXT,

    -- Audit
    recorded_by_start_id UUID REFERENCES users(id),
    recorded_by_end_id UUID REFERENCES users(id),
    recorded_at_start TIMESTAMP,
    recorded_at_end TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_START',

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_utility_readings_booking ON utility_readings(booking_id);
CREATE INDEX idx_utility_readings_room ON utility_readings(room_id);

CREATE TABLE utility_price_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties(id),
    water_price_per_unit NUMERIC(15,2) NOT NULL DEFAULT 15000,
    electric_tier1_price NUMERIC(15,2) DEFAULT 1728,
    electric_tier2_price NUMERIC(15,2) DEFAULT 1786,
    electric_tier3_price NUMERIC(15,2) DEFAULT 2074,
    electric_tier4_price NUMERIC(15,2) DEFAULT 2612,
    electric_tier5_price NUMERIC(15,2) DEFAULT 2919,
    electric_tier6_price NUMERIC(15,2) DEFAULT 3015,
    use_fixed_electric_price BOOLEAN DEFAULT TRUE,
    fixed_electric_price NUMERIC(15,2) DEFAULT 3500,
    effective_from DATE,
    effective_to DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_utility_price_property ON utility_price_configs(property_id);
