package com.hotelchain.pro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Tên đăng nhập không được trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được trống")
    private String password;

    private String deviceId;        // Optional device UUID
    private String deviceName;      // "iPhone 15 Pro"
    private String fcmToken;        // Firebase push token
}
