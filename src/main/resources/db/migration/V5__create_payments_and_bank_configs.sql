-- V5__create_payments_and_bank_configs.sql
-- Tạo bảng bank_configs và payments

CREATE TABLE bank_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL UNIQUE REFERENCES properties(id),
    account_holder_name VARCHAR(255) NOT NULL,
    account_number VARCHAR(255) NOT NULL,   -- AES encrypted
    bank_code VARCHAR(20) NOT NULL,
    bank_name VARCHAR(100),
    bank_bin VARCHAR(20),
    branch VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    template_description VARCHAR(500),
    require_utility_photo BOOLEAN DEFAULT TRUE,
    require_utility_input BOOLEAN DEFAULT TRUE,
    static_qr_image_key VARCHAR(500),
    qr_generated_at TIMESTAMP,
    auto_confirm_enabled BOOLEAN DEFAULT FALSE,
    webhook_secret VARCHAR(500),           -- AES encrypted
    confirm_timeout_minutes INTEGER DEFAULT 30,
    configured_by_id UUID REFERENCES users(id),
    last_updated TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id),
    amount NUMERIC(15,2) NOT NULL,
    method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    qr_code TEXT,
    qr_image_key VARCHAR(500),
    viet_qr_code TEXT,
    viet_qr_data_url TEXT,
    transfer_content VARCHAR(500),
    paid_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    auto_confirmed BOOLEAN DEFAULT FALSE,
    confirmed_by_id UUID REFERENCES users(id),
    refund_amount NUMERIC(15,2),
    refunded_at TIMESTAMP,
    refund_reason TEXT,
    notes TEXT,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE UNIQUE INDEX idx_payments_idempotency ON payments(idempotency_key) WHERE idempotency_key IS NOT NULL;
