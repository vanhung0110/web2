package com.hotelchain.pro.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExtendBookingRequest {
    @NotNull(message = "Thời gian check-out mới không được để trống")
    private LocalDateTime newCheckOutPlan;
}
