-- V10__seed_bank_list_and_admin.sql
-- Seed data mẫu: danh sách ngân hàng VN + tài khoản SUPER_ADMIN mặc định

-- Seed initial SUPER_ADMIN user
-- Password: Admin@123456 (BCrypt hash)
INSERT INTO users (id, username, email, password, full_name, role, enabled, account_locked, must_change_password, password_changed_at, created_at, version, deleted)
VALUES (
    gen_random_uuid(),
    'superadmin',
    'superadmin@hotelchain.vn',
    '$2a$12$LnGHbULOb27uGX1qlUm7peRzwMmCkj93JLv7DpT7R2Rl0ECEq.A7e', -- Admin@123456
    'Super Administrator',
    'SUPER_ADMIN',
    TRUE,
    FALSE,
    TRUE,  -- Must change password on first login
    NOW(),
    NOW(),
    0,
    FALSE
);

-- Bank list reference data (thông tin ngân hàng VN)
CREATE TABLE bank_list (
    bank_code VARCHAR(20) PRIMARY KEY,
    bin VARCHAR(20) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    short_name VARCHAR(50),
    logo_url VARCHAR(500),
    is_supported BOOLEAN DEFAULT TRUE
);

INSERT INTO bank_list (bank_code, bin, display_name, short_name) VALUES
('VCB', '970436', 'Vietcombank', 'VCB'),
('BIDV', '970418', 'BIDV', 'BIDV'),
('VTB', '970415', 'Vietinbank', 'VTB'),
('AGR', '970405', 'Agribank', 'AGR'),
('MB', '970422', 'MB Bank', 'MB'),
('TCB', '970407', 'Techcombank', 'TCB'),
('ACB', '970416', 'ACB', 'ACB'),
('VPB', '970432', 'VPBank', 'VPB'),
('TPB', '970423', 'TPBank', 'TPB'),
('STB', '970403', 'Sacombank', 'STB'),
('EIB', '970431', 'Eximbank', 'EIB'),
('HDB', '970437', 'HDBank', 'HDB'),
('OCB', '970448', 'OCB', 'OCB'),
('SHB', '970443', 'SHB', 'SHB'),
('BAB', '970409', 'BAC A BANK', 'BAB'),
('MSB', '970426', 'MSB', 'MSB'),
('NAB', '970428', 'Nam A Bank', 'NAB'),
('PGB', '970430', 'PG Bank', 'PGB'),
('SEAB', '970440', 'SeABank', 'SEAB'),
('VIB', '970441', 'VIB', 'VIB'),
('LPB', '970449', 'LienVietPostBank', 'LPB'),
('KLB', '970452', 'Kienlong Bank', 'KLB'),
('BVB', '970454', 'BaoViet Bank', 'BVB'),
('VIETBANK', '970433', 'VietBank', 'VIETBANK'),
('CAKE', '546034', 'CAKE by VPBank', 'CAKE'),
('TIMO', '963388', 'Timo', 'TIMO'),
('MOMO', 'MOMO', 'MoMo', 'MOMO'),
('ZALOPAY', 'ZALO', 'ZaloPay', 'ZALOPAY');
