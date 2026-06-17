package com.hotelchain.pro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Mật khẩu hiện tại không được trống")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được trống")
    @Size(min = 8, message = "Mật khẩu mới phải ít nhất 8 ký tự")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được trống")
    private String confirmPassword;
}
