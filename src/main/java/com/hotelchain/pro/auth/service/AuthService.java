package com.hotelchain.pro.auth.service;

import com.hotelchain.pro.auth.dto.*;
import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.auth.jwt.JwtTokenProvider;
import com.hotelchain.pro.auth.repository.UserRepository;
import com.hotelchain.pro.common.exception.AuthException;
import com.hotelchain.pro.common.exception.HotelChainException;
import com.hotelchain.pro.security.service.TwoFactorService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Auth Service — xử lý đăng nhập, refresh token, logout, đổi mật khẩu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TwoFactorService twoFactorService;
    private final com.hotelchain.pro.security.service.RateLimitService rateLimitService;
    private final com.hotelchain.pro.property.repository.PropertyRepository propertyRepository;

    /**
     * Đăng nhập — trả về access token + refresh token.
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        // Rate limiting: 5 lần login thất bại / IP / 15 phút
        rateLimitService.checkLoginRateLimit(ipAddress, request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(AuthException::invalidCredentials);

        // Check account lock
        if (!user.isAccountNonLocked()) {
            throw AuthException.accountLocked();
        }

        // Authenticate
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            handleFailedLogin(user);
            throw AuthException.invalidCredentials();
        }

        // Check 2FA
        if (user.getTwoFactorEnabled()) {
            throw AuthException.twoFactorRequired();
        }

        // Reset failed attempts on success
        userRepository.resetLoginAttempts(user.getId());
        rateLimitService.resetLoginRateLimit(ipAddress, request.getUsername());

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);

        // Add FCM token if provided
        if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
            user.getFcmTokens().add(request.getFcmToken());
        }
        userRepository.save(user);

        String deviceId = request.getDeviceId() != null ? request.getDeviceId() : UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.generateAccessToken(user, deviceId, request.getFcmToken());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString(), deviceId);

        return buildLoginResponse(user, accessToken, refreshToken);
    }

    /**
     * Refresh token — cấp access token mới từ refresh token hợp lệ.
     */
    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(refreshToken);
        } catch (Exception e) {
            throw AuthException.tokenInvalid();
        }

        if (!"refresh".equals(claims.get("type"))) {
            throw AuthException.tokenInvalid();
        }

        String userId = claims.getSubject();
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(AuthException::tokenInvalid);

        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw AuthException.accountLocked();
        }

        // Revoke old refresh token
        jwtTokenProvider.revokeToken(claims.getId());

        String deviceId = claims.get("deviceId", String.class);
        String newAccessToken = jwtTokenProvider.generateAccessToken(user, deviceId, null);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString(), deviceId);

        return buildLoginResponse(user, newAccessToken, newRefreshToken);
    }

    /**
     * Logout — revoke current token.
     */
    public void logout(String token) {
        try {
            Claims claims = jwtTokenProvider.parseToken(token);
            jwtTokenProvider.revokeToken(claims.getId());
        } catch (Exception e) {
            log.warn("Logout error: {}", e.getMessage());
        }
    }

    /**
     * Logout all devices — revoke tất cả token của user.
     */
    @Transactional(readOnly = true)
    public void logoutAll(UUID userId) {
        jwtTokenProvider.revokeAllUserTokens(userId);
    }

    /**
     * Đổi mật khẩu.
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HotelChainException("NOT_FOUND", "Người dùng không tồn tại", HttpStatus.NOT_FOUND));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new HotelChainException("AUTH_001", "Mật khẩu hiện tại không đúng");
        }

        // Confirm match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new HotelChainException("VALIDATION_ERROR", "Mật khẩu xác nhận không khớp");
        }

        // Check password history (không trùng 5 mật khẩu gần nhất)
        boolean isReused = user.getPasswordHistory().stream()
                .anyMatch(oldHash -> passwordEncoder.matches(request.getNewPassword(), oldHash));
        if (isReused) {
            throw new HotelChainException("PASSWORD_REUSED",
                    "Mật khẩu mới không được trùng với 5 mật khẩu gần nhất");
        }

        // Update password
        String newHash = passwordEncoder.encode(request.getNewPassword());
        user.getPasswordHistory().add(0, user.getPassword());
        if (user.getPasswordHistory().size() > 5) {
            user.getPasswordHistory().subList(5, user.getPasswordHistory().size()).clear();
        }
        user.setPassword(newHash);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Logout all other sessions
        jwtTokenProvider.revokeAllUserTokens(userId);
    }

    @Transactional
    public void sendPasswordResetEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new HotelChainException("NOT_FOUND", "Email không tồn tại trong hệ thống", HttpStatus.NOT_FOUND));
        // Mock sending password reset email
        log.info("Sending password reset email to {}. Reset token: {}", email, user.getId());
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Mock token validation: token is the user's ID
        UUID userId;
        try {
            userId = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            // If token is not UUID, look up user by username/email
            User user = userRepository.findByUsername(token)
                    .or(() -> userRepository.findByEmail(token))
                    .orElseThrow(() -> new HotelChainException("INVALID_TOKEN", "Token khôi phục mật khẩu không hợp lệ"));
            userId = user.getId();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HotelChainException("NOT_FOUND", "Người dùng không tồn tại"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Logout all devices
        jwtTokenProvider.revokeAllUserTokens(user.getId());
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked for user {} after {} failed attempts", user.getUsername(), attempts);
        }
        userRepository.save(user);
    }

    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
        List<LoginResponse.PropertyInfo> propertyInfos = user.getAssignedPropertyIds().stream()
                .map(propId -> propertyRepository.findById(propId)
                        .map(p -> LoginResponse.PropertyInfo.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .code(p.getCode())
                                .build())
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirySeconds())
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .assignedProperties(propertyInfos)
                        .avatarUrl(user.getAvatarKey()) // TODO: presigned URL from MinIO
                        .mustChangePassword(user.getMustChangePassword())
                        .build())
                .build();
    }
}
