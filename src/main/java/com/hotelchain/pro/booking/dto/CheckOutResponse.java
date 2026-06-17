package com.hotelchain.pro.booking.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CheckOutResponse {
    private String bookingCode;
    private String roomNumber;
    private BigDecimal roomFee;
    private Double waterUsage;
    private BigDecimal waterCost;
    private Double electricUsage;
    private BigDecimal electricCost;
    private BigDecimal serviceFee;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private String qrCodeUrl;       // dynamic QR URL
    private String paymentStatus;   // PENDING, PAID
}
