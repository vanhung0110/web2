package com.hotelchain.pro.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateRoomRequest {
    @NotBlank(message = "Số phòng không được để trống")
    private String roomNumber;

    private Integer floor = 1;

    @NotNull(message = "Giá thuê tháng không được để trống")
    @Min(value = 0, message = "Giá thuê tháng phải lớn hơn hoặc bằng 0")
    private BigDecimal monthlyRent;

    @Min(value = 0, message = "Giá thuê ngày phải lớn hơn hoặc bằng 0")
    private BigDecimal dailyRent;

    private String description;

    @NotNull(message = "Chi nhánh không được để trống")
    private java.util.UUID branchId;
}
