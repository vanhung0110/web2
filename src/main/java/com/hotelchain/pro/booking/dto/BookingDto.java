package com.hotelchain.pro.booking.dto;

import com.hotelchain.pro.common.enums.BookingSource;
import com.hotelchain.pro.common.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingDto {
    private UUID id;
    private String bookingCode;
    private UUID roomId;
    private String roomNumber;
    private UUID guestId;
    private String guestName;
    private UUID createdByUserId;
    private UUID propertyId;
    private LocalDateTime checkInPlan;
    private LocalDateTime checkOutPlan;
    private LocalDateTime actualCheckIn;
    private LocalDateTime actualCheckOut;
    private BookingStatus status;
    private BookingSource source;
    private Integer adultsCount;
    private Integer childrenCount;
    private BigDecimal roomRatePerNight;
    private BigDecimal totalRoomFee;
    private BigDecimal utilityCost;
    private BigDecimal serviceFee;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private String specialRequests;
    private String internalNote;
}
