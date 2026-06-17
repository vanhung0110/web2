package com.hotelchain.pro.utility.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EndReadingRequest {
    @NotNull(message = "Mã đặt phòng không được để trống")
    private UUID bookingId;

    @NotNull(message = "Chỉ số nước kết thúc không được để trống")
    private Double waterIndexEnd;

    private String waterPhotoEndKey;

    @NotNull(message = "Chỉ số điện kết thúc không được để trống")
    private Double electricIndexEnd;

    private String electricPhotoEndKey;
}
