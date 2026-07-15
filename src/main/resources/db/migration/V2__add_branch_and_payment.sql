CREATE TABLE branches (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Thêm branch_id vào bảng rooms
ALTER TABLE rooms ADD COLUMN branch_id UUID;
ALTER TABLE rooms ADD CONSTRAINT fk_rooms_branch FOREIGN KEY (branch_id) REFERENCES branches(id);

-- Thêm thông tin ngân hàng vào cài đặt chung (giả sử bảng utility_prices là cài đặt chung)
ALTER TABLE utility_prices ADD COLUMN bank_name VARCHAR(100);
ALTER TABLE utility_prices ADD COLUMN bank_account VARCHAR(100);
ALTER TABLE utility_prices ADD COLUMN account_name VARCHAR(255);

-- Thêm trạng thái thanh toán vào hóa đơn
ALTER TABLE utility_reports ADD COLUMN is_paid BOOLEAN DEFAULT FALSE;
ALTER TABLE utility_reports ADD COLUMN payment_date TIMESTAMP;
