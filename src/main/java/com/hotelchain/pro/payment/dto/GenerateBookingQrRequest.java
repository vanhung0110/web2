package com.hotelchain.pro.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class GenerateBookingQrRequest {
    @NotNull(message = "Mã đặt phòng không được để trống")
    private UUID bookingId;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    private BigDecimal amount;
}
