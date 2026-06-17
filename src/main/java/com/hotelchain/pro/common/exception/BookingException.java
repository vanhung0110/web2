package com.hotelchain.pro.common.exception;

import org.springframework.http.HttpStatus;

/** BOOKING_001 — BOOKING_003 */
public class BookingException extends HotelChainException {
    public BookingException(String code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }

    public static BookingException roomAlreadyBooked() {
        return new BookingException("BOOKING_001", "Phòng đã được đặt trong thời gian này");
    }

    public static BookingException roomNotFound() {
        return new BookingException("BOOKING_002", "Phòng không tồn tại") {
            @Override
            public HttpStatus getHttpStatus() { return HttpStatus.NOT_FOUND; }
        };
    }

    public static BookingException invalidStatus(String currentStatus, String requiredStatus) {
        return new BookingException("BOOKING_003",
                String.format("Trạng thái booking không hợp lệ. Hiện tại: %s, yêu cầu: %s",
                        currentStatus, requiredStatus));
    }
}
