-- Thêm giá theo ngày vào bảng phòng
ALTER TABLE rooms ADD COLUMN daily_rent DECIMAL(15,2) DEFAULT 0.0;

-- Tạo bảng bookings
CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id),
    guest_name VARCHAR(255) NOT NULL,
    guest_phone VARCHAR(50),
    guest_identity VARCHAR(50),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_price DECIMAL(15,2),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
