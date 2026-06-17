package com.hotelchain.pro.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateBookingRequest {
    @NotNull(message = "Mã phòng không được để trống")
    private UUID roomId;

    // Guest details (hoặc truyền guestId nếu là khách cũ, hoặc nhập thông tin nếu là khách mới)
    private UUID guestId;

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    private String email;
    private String idNumber;
    private String idType; // CCCD, CMND, PASSPORT

    @NotNull(message = "Thời gian nhận phòng dự kiến không được để trống")
    private LocalDateTime checkInPlan;

    @NotNull(message = "Thời gian trả phòng dự kiến không được để trống")
    private LocalDateTime checkOutPlan;

    private Integer adultsCount = 1;
    private Integer childrenCount = 0;

    @NotNull(message = "Giá phòng mỗi đêm không được để trống")
    private BigDecimal roomRatePerNight;

    private BigDecimal depositAmount = BigDecimal.ZERO;
    private String specialRequests;
    private String source; // WALK_IN, PHONE, WEBSITE, OTA
}
