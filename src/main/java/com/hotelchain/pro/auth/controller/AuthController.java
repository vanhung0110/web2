package com.hotelchain.pro.auth.controller;

import com.hotelchain.pro.auth.dto.ChangePasswordRequest;
import com.hotelchain.pro.auth.dto.LoginRequest;
import com.hotelchain.pro.auth.dto.LoginResponse;
import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.auth.service.AuthService;
import com.hotelchain.pro.common.response.ApiResponse;
import com.hotelchain.pro.security.dto.TwoFactorSetupResponse;
import com.hotelchain.pro.security.service.TwoFactorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Module 1 — Auth Controller
 * Handles: login, refresh, logout, logout-all, change-password, forgot-password, reset-password, 2FA
 */
@Tag(name = "Authentication", description = "Xác thực & quản lý phiên đăng nhập")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TwoFactorService twoFactorService;

    /** POST /api/v1/auth/login — Đăng nhập */
    @Operation(summary = "Đăng nhập hệ thống")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        LoginResponse response = authService.login(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    /** POST /api/v1/auth/refresh — Làm mới token */
    @Operation(summary = "Làm mới access token bằng refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestBody RefreshRequest request) {
        LoginResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", response));
    }

    /** POST /api/v1/auth/logout — Đăng xuất */
    @Operation(summary = "Đăng xuất khỏi thiết bị hiện tại")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String token = extractToken(request);
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.noContent("Đăng xuất thành công"));
    }

    /** POST /api/v1/auth/logout-all — Đăng xuất tất cả thiết bị */
    @Operation(summary = "Đăng xuất tất cả thiết bị")
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(@AuthenticationPrincipal User user) {
        authService.logoutAll(user.getId());
        return ResponseEntity.ok(ApiResponse.noContent("Đã đăng xuất tất cả thiết bị"));
    }

    /** POST /api/v1/auth/change-password — Đổi mật khẩu */
    @Operation(summary = "Đổi mật khẩu")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.noContent("Đổi mật khẩu thành công. Vui lòng đăng nhập lại."));
    }

    /** POST /api/v1/auth/forgot-password — Quên mật khẩu */
    @Operation(summary = "Yêu cầu đặt lại mật khẩu qua email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.sendPasswordResetEmail(request.email());
        return ResponseEntity.ok(ApiResponse.noContent(
                "Email đặt lại mật khẩu đã được gửi (nếu email tồn tại trong hệ thống)"));
    }

    /** POST /api/v1/auth/reset-password — Đặt lại mật khẩu */
    @Operation(summary = "Đặt lại mật khẩu bằng token")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.noContent("Đặt lại mật khẩu thành công"));
    }

    /** POST /api/v1/auth/2fa/setup — Bật xác thực 2 bước */
    @Operation(summary = "Thiết lập xác thực 2 bước (2FA TOTP)")
    @PostMapping("/2fa/setup")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup2FA(
            @AuthenticationPrincipal User user) {
        TwoFactorSetupResponse response = twoFactorService.setupTwoFactor(user);
        return ResponseEntity.ok(ApiResponse.success("Quét QR bằng Google Authenticator", response));
    }

    /** POST /api/v1/auth/2fa/verify — Xác nhận OTP 2FA */
    @Operation(summary = "Xác nhận OTP để kích hoạt 2FA")
    @PostMapping("/2fa/verify")
    public ResponseEntity<ApiResponse<LoginResponse>> verify2FA(
            @AuthenticationPrincipal User user,
            @RequestBody TwoFactorVerifyRequest request) {
        LoginResponse response = twoFactorService.verifyAndComplete(user, request.otp(), request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("Xác thực 2 bước thành công", response));
    }

    // ===== Helper =====

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return "";
    }

    // ===== Inner Record DTOs =====
    record RefreshRequest(String refreshToken) {}
    record ForgotPasswordRequest(String email) {}
    record ResetPasswordRequest(String token, String newPassword) {}
    record TwoFactorVerifyRequest(int otp, String refreshToken) {}
}
