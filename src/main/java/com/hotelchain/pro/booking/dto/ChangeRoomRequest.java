package com.hotelchain.pro.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ChangeRoomRequest {
    @NotNull(message = "Mã phòng mới không được để trống")
    private UUID newRoomId;
}
