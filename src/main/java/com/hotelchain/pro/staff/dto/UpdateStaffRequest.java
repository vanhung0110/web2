package com.hotelchain.pro.staff.dto;

import com.hotelchain.pro.common.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateStaffRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotNull(message = "Vai trò không được để trống")
    private Role role;

    @NotNull(message = "Mã chi nhánh không được để trống")
    private UUID propertyId;

    private String address;
    private String notes;
}
