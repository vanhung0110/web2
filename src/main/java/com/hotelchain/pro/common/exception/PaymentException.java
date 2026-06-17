package com.hotelchain.pro.common.exception;

import org.springframework.http.HttpStatus;

/** PAYMENT_001 — PAYMENT_004 */
public class PaymentException extends HotelChainException {
    public PaymentException(String code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }

    public static PaymentException waterReadingMissing() {
        return new PaymentException("PAYMENT_001", "Chưa ghi chỉ số đồng hồ nước");
    }

    public static PaymentException utilityPhotoMissing() {
        return new PaymentException("PAYMENT_002", "Chưa upload ảnh đồng hồ");
    }

    public static PaymentException invalidWaterReading() {
        return new PaymentException("PAYMENT_003", "Chỉ số nước không hợp lệ (cuối < đầu)");
    }

    public static PaymentException bankConfigNotConfigured() {
        return new PaymentException("PAYMENT_004", "Cấu hình ngân hàng chưa được thiết lập");
    }
}
