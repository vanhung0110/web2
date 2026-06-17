package com.hotelchain.pro.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerateQRRequest {
    private String accountNo;
    private String accountName;
    private String bankBin;
    private String transferContent;
    private long amount;
    private String bookingCode;
}
