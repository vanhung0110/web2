package com.hotelchain.pro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class CreateTenantRequest {
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotNull(message = "Phòng không được để trống")
    private UUID roomId;
}
