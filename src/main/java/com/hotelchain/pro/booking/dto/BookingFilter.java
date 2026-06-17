package com.hotelchain.pro.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BookingFilter {
    private UUID propertyId;
    private String status;
    private String searchQuery;
    private String from;
    private String to;
}
