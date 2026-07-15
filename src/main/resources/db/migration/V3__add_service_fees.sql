-- Thêm phí dịch vụ vào bảng giá điện nước
ALTER TABLE utility_prices ADD COLUMN internet_fee DECIMAL(15,2) DEFAULT 0.0;
ALTER TABLE utility_prices ADD COLUMN trash_fee DECIMAL(15,2) DEFAULT 0.0;

-- Thêm lưu trữ phí dịch vụ vào hóa đơn để không bị ảnh hưởng khi giá thay đổi
ALTER TABLE utility_reports ADD COLUMN internet_fee DECIMAL(15,2) DEFAULT 0.0;
ALTER TABLE utility_reports ADD COLUMN trash_fee DECIMAL(15,2) DEFAULT 0.0;
