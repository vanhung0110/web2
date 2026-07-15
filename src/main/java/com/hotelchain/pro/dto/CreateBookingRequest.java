package com.hotelchain.pro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateBookingRequest {
    
    @NotNull(message = "PhAng khA'ng `c ` tr`ng")
    private UUID roomId;

    @NotBlank(message = "TAn khAch hAng khA'ng `c ` tr`ng")
    private String guestName;

    private String guestPhone;
    
    private String guestIdentity;

    @NotNull(message = "NgAy check-in khA'ng `c ` tr`ng")
    private LocalDate checkInDate;

    @NotNull(message = "NgAy check-out khA'ng `c ` tr`ng")
    private LocalDate checkOutDate;
}
