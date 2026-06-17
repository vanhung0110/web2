package com.hotelchain.pro.property.dto;

import com.hotelchain.pro.common.enums.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePropertyRequest {
    @NotBlank(message = "Tên chi nhánh không được để trống")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    private String ward;
    private String district;
    private String city;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String email;
    private String description;

    @NotNull(message = "Loại hình lưu trú không được để trống")
    private PropertyType type;

    private Integer starRating;
}
