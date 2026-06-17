package com.hotelchain.pro.staff.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AttendanceDto {
    private UUID staffId;
    private String staffName;
    private Double scheduledHours;
    private Double actualHours;
    private Long lateMinutes;
    private Double overtimeHours;
    private Integer daysWorked;
}
