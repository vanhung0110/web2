package com.hotelchain.pro.utility.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class StartReadingRequest {
    @NotNull(message = "Mã đặt phòng không được để trống")
    private UUID bookingId;

    @NotNull(message = "Chỉ số nước bắt đầu không được để trống")
    private Double waterIndexStart;

    private String waterPhotoStartKey;

    @NotNull(message = "Chỉ số điện bắt đầu không được để trống")
    private Double electricIndexStart;

    private String electricPhotoStartKey;
}
