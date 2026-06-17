package com.hotelchain.pro.property.dto;

import com.hotelchain.pro.common.enums.PropertyType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PropertyDto {
    private UUID id;
    private String name;
    private String code;
    private String address;
    private String ward;
    private String district;
    private String city;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String email;
    private String description;
    private PropertyType type;
    private Integer starRating;
    private Boolean isActive;
    private List<String> imageKeys;
}
