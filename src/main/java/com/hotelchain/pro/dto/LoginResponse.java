package com.hotelchain.pro.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class LoginResponse {
    private UUID userId;
    private String phone;
    private String fullName;
    private String role;
    private String token;
    // Thông tin phòng (cho USER)
    private UUID roomId;
    private String roomNumber;
}
