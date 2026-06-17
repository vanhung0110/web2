package com.hotelchain.pro.booking.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckOutRequest {
    private LocalDateTime actualCheckOut;
    private Double waterIndexEnd;
    private String waterPhotoEndKey;
    private Double electricIndexEnd;
    private String electricPhotoEndKey;
    private String checkoutNotes;
    private String roomCondition; // GOOD, DAMAGED, NEEDS_CLEANING
}
