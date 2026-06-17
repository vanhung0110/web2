package com.hotelchain.pro.payment.dto;

import com.hotelchain.pro.common.enums.BankCode;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BankConfigDto {
    private UUID id;
    private UUID propertyId;
    private String accountHolderName;
    private String accountNumber;
    private BankCode bankCode;
    private String bankName;
    private String bankBin;
    private String branch;
    private Boolean isActive;
    private String templateDescription;
    private Boolean requireUtilityPhoto;
    private Boolean requireUtilityInput;
    private String staticQrImageKey;
    private Boolean autoConfirmEnabled;
    private Integer confirmTimeoutMinutes;
}
