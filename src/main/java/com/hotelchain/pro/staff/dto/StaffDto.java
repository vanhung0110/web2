package com.hotelchain.pro.staff.dto;

import com.hotelchain.pro.common.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class StaffDto {
    private UUID id;
    private UUID userId;
    private String username;
    private UUID propertyId;
    private String propertyName;
    private String fullName;
    private String phone;
    private String address;
    private Role role;
    private Boolean isActive;
    private LocalDate startDate;
    private LocalDate endDate;
    private String avatarKey;
    private String notes;
}
