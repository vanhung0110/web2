package com.hotelchain.pro.staff.dto;

import com.hotelchain.pro.common.enums.ShiftType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ShiftDto {
    private UUID id;
    private UUID staffId;
    private String staffName;
    private UUID propertyId;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private ShiftType type;
    private String notes;
    private Boolean isOvertime;
    private Double clockInLatitude;
    private Double clockInLongitude;
    private Double clockOutLatitude;
    private Double clockOutLongitude;
}
