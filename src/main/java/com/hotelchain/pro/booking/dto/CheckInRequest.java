package com.hotelchain.pro.booking.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CheckInRequest {
    private LocalDateTime actualCheckIn;
    private String guestIdNumber;
    private String guestIdType;
    private String guestIdImageFrontKey;
    private String guestIdImageBackKey;
    private Double waterIndexStart;
    private String waterPhotoStartKey;
    private Double electricIndexStart;
    private String electricPhotoStartKey;
    private BigDecimal depositAmount;
    private String depositPaymentMethod; // CASH, BANK_TRANSFER, CARD
    private String notes;
}
