-- ============================================
-- Hệ thống Quản Lý Nhà Trọ — Schema v1
-- ============================================

-- Bảng users (admin + người thuê)
CREATE TABLE users (
    id UUID PRIMARY KEY,
    phone VARCHAR(15) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);

-- Bảng rooms (phòng trọ)
CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    floor INTEGER NOT NULL DEFAULT 1,
    monthly_rent NUMERIC(15,2) NOT NULL DEFAULT 0,
    description TEXT,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_rooms_number ON rooms(room_number);

-- Bảng tenants (gán người thuê → phòng)
CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    move_in_date DATE NOT NULL DEFAULT CURRENT_DATE,
    move_out_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenants_user ON tenants(user_id);
CREATE INDEX idx_tenants_room ON tenants(room_id);
-- (Bỏ index độc quyền do H2 không hỗ trợ partial index, app đã có logic kiểm tra)

-- Bảng utility_reports (báo cáo điện nước hàng tháng)
CREATE TABLE utility_reports (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    room_id UUID NOT NULL REFERENCES rooms(id),
    report_month INTEGER NOT NULL CHECK (report_month BETWEEN 1 AND 12),
    report_year INTEGER NOT NULL,

    water_old DOUBLE PRECISION NOT NULL DEFAULT 0,
    water_new DOUBLE PRECISION NOT NULL DEFAULT 0,
    water_photo_key VARCHAR(500),

    electric_old DOUBLE PRECISION NOT NULL DEFAULT 0,
    electric_new DOUBLE PRECISION NOT NULL DEFAULT 0,
    electric_photo_key VARCHAR(500),

    water_usage DOUBLE PRECISION,
    electric_usage DOUBLE PRECISION,

    water_cost NUMERIC(15,2),
    electric_cost NUMERIC(15,2),
    room_rent NUMERIC(15,2),
    total_cost NUMERIC(15,2),

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason TEXT,

    submitted_by UUID REFERENCES users(id),
    submitted_at TIMESTAMP DEFAULT NOW(),
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP,

    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_tenant ON utility_reports(tenant_id);
CREATE INDEX idx_reports_room ON utility_reports(room_id);
CREATE INDEX idx_reports_status ON utility_reports(status);
CREATE INDEX idx_reports_month ON utility_reports(report_year, report_month);
CREATE UNIQUE INDEX idx_reports_room_month ON utility_reports(room_id, report_month, report_year);

-- Bảng utility_prices (đơn giá điện nước)
CREATE TABLE utility_prices (
    id UUID PRIMARY KEY,
    water_price_per_unit NUMERIC(15,2) NOT NULL DEFAULT 15000,
    electric_price_per_unit NUMERIC(15,2) NOT NULL DEFAULT 3500,
    effective_from DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- Seed data
-- ============================================
INSERT INTO users (id, phone, full_name, role) VALUES (RANDOM_UUID(), '0962750432', 'Admin Nhà Trọ', 'ADMIN');
INSERT INTO utility_prices (id, water_price_per_unit, electric_price_per_unit) VALUES (RANDOM_UUID(), 15000, 3500);
