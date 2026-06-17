package com.hotelchain.pro.payment.dto;

import com.hotelchain.pro.common.enums.BankCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateBankConfigRequest {
    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    private String accountHolderName;

    @NotBlank(message = "Số tài khoản không được để trống")
    private String accountNumber;

    @NotNull(message = "Ngân hàng không được để trống")
    private BankCode bankCode;

    private String branch;
    private String templateDescription;
    private Boolean requireUtilityPhoto = true;
    private Boolean requireUtilityInput = true;
    private Boolean autoConfirmEnabled = false;
    private Integer confirmTimeoutMinutes = 30;
}
