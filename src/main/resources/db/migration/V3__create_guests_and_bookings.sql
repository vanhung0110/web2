-- V3__create_guests_and_bookings.sql
-- Tạo bảng guests và bookings

CREATE TABLE guests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(255),
    id_number VARCHAR(30),
    id_type VARCHAR(20),
    id_image_front_key VARCHAR(500),
    id_image_back_key VARCHAR(500),
    nationality VARCHAR(10) DEFAULT 'VN',
    address VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_guests_phone ON guests(phone);
CREATE INDEX idx_guests_id_number ON guests(id_number);
CREATE INDEX idx_guests_email ON guests(email);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    tenant_id UUID,
    avatar_key VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    password_changed_at TIMESTAMP,
    must_change_password BOOLEAN DEFAULT FALSE,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_tenant ON users(tenant_id);

CREATE TABLE user_property_access (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, property_id)
);

CREATE TABLE user_password_history (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    seq INTEGER NOT NULL
);

CREATE TABLE user_fcm_tokens (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fcm_token VARCHAR(500) NOT NULL
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_code VARCHAR(30) NOT NULL UNIQUE,
    room_id UUID NOT NULL REFERENCES rooms(id),
    guest_id UUID NOT NULL REFERENCES guests(id),
    created_by_user_id UUID REFERENCES users(id),
    property_id UUID NOT NULL REFERENCES properties(id),
    check_in_plan TIMESTAMP NOT NULL,
    check_out_plan TIMESTAMP NOT NULL,
    actual_check_in TIMESTAMP,
    actual_check_out TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    source VARCHAR(20) DEFAULT 'WALK_IN',
    adults_count INTEGER DEFAULT 1,
    children_count INTEGER DEFAULT 0,
    room_rate_per_night NUMERIC(15,2),
    total_room_fee NUMERIC(15,2),
    utility_cost NUMERIC(15,2) DEFAULT 0,
    service_fee NUMERIC(15,2) DEFAULT 0,
    discount NUMERIC(15,2) DEFAULT 0,
    total_amount NUMERIC(15,2),
    deposit_amount NUMERIC(15,2) DEFAULT 0,
    remaining_amount NUMERIC(15,2),
    special_requests TEXT,
    internal_note TEXT,
    self_check_in_token VARCHAR(255),
    self_check_in_token_expiry TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_bookings_room_dates ON bookings(room_id, check_in_plan, check_out_plan);
CREATE INDEX idx_bookings_status ON bookings(status) WHERE status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN');
CREATE INDEX idx_bookings_property ON bookings(property_id, created_at DESC);
CREATE INDEX idx_bookings_guest ON bookings(guest_id);
CREATE UNIQUE INDEX idx_bookings_code ON bookings(booking_code);

-- Sequence để sinh booking code
CREATE SEQUENCE booking_number_seq START WITH 1 INCREMENT BY 1;
