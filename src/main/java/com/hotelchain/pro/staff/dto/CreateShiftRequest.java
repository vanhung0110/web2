package com.hotelchain.pro.staff.dto;

import com.hotelchain.pro.common.enums.ShiftType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateShiftRequest {
    @NotNull(message = "Nhân viên không được để trống")
    private UUID staffId;

    @NotNull(message = "Mã chi nhánh không được để trống")
    private UUID propertyId;

    @NotNull(message = "Thời gian bắt đầu ca không được để trống")
    private LocalDateTime scheduledStart;

    @NotNull(message = "Thời gian kết thúc ca không được để trống")
    private LocalDateTime scheduledEnd;

    @NotNull(message = "Loại ca làm việc không được để trống")
    private ShiftType type;

    private String notes;
    private Boolean isOvertime = false;
}
