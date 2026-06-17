package com.hotelchain.pro.common.exception;

import org.springframework.http.HttpStatus;

/** UTILITY_001 — UTILITY_004 */
public class UtilityException extends HotelChainException {
    public UtilityException(String code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }

    public static UtilityException photoRequired() {
        return new UtilityException("UTILITY_001", "Ảnh đồng hồ bắt buộc phải upload");
    }

    public static UtilityException manualInputRequired() {
        return new UtilityException("UTILITY_002", "Chỉ số tay bắt buộc phải nhập");
    }

    public static UtilityException stalePhoto() {
        return new UtilityException("UTILITY_003", "Ảnh quá cũ (chụp cách đây > 60 phút). Vui lòng chụp lại.");
    }

    public static UtilityException ocrDiscrepancy(double ocrValue, double manualValue) {
        return new UtilityException("UTILITY_004",
                String.format("Chênh lệch lớn giữa ảnh OCR (%.1f) và nhập tay (%.1f). Vui lòng kiểm tra lại.",
                        ocrValue, manualValue));
    }
}
