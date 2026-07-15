package com.hotelchain.pro.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateBookingRequest {
    
    @NotNull(message = "Phòng không được để trống")
    private UUID roomId;

    @NotBlank(message = "Tên khách không được để trống")
    private String guestName;

    private String guestPhone;
    
    private String guestIdentity;

    @NotNull(message = "Ngày check-in không được để trống")
    private LocalDate checkInDate;

    @NotNull(message = "Ngày check-out không được để trống")
    private LocalDate checkOutDate;
}
