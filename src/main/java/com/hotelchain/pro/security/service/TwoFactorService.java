package com.hotelchain.pro.security.service;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.auth.jwt.JwtTokenProvider;
import com.hotelchain.pro.auth.repository.UserRepository;
import com.hotelchain.pro.auth.dto.LoginResponse;
import com.hotelchain.pro.security.dto.TwoFactorSetupResponse;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two-Factor Authentication (2FA TOTP) — tương thích Google Authenticator.
 * Thư viện: com.warrenstrange:googleauth
 */
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private static final String ISSUER = "HotelChain Pro";

    private final GoogleAuthenticator googleAuthenticator;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Sinh secret key và QR code URI để setup 2FA.
     */
    @Transactional
    public TwoFactorSetupResponse setupTwoFactor(User user) {
        GoogleAuthenticatorKey credentials = googleAuthenticator.createCredentials();
        String secret = credentials.getKey();

        // Lưu secret tạm thời (chưa enable cho đến khi verify)
        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        String qrCodeUri = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                ISSUER,
                user.getEmail(),
                credentials
        );

        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .qrCodeUri(qrCodeUri)
                .issuer(ISSUER)
                .build();
    }

    /**
     * Xác nhận OTP để kích hoạt 2FA.
     */
    @Transactional
    public LoginResponse verifyAndComplete(User user, int otp, String pendingRefreshToken) {
        if (!verifyOtp(user.getTwoFactorSecret(), otp)) {
            throw new com.hotelchain.pro.common.exception.HotelChainException("2FA_INVALID", "OTP không hợp lệ");
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        // Sau khi verify 2FA, generate proper tokens
        String deviceId = java.util.UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.generateAccessToken(user, deviceId, null);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString(), deviceId);

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
                        .mustChangePassword(user.getMustChangePassword())
                        .build())
                .build();
    }

    /**
     * Verify OTP code.
     */
    public boolean verifyOtp(String secret, int otp) {
        return googleAuthenticator.authorize(secret, otp);
    }

    /**
     * Tắt 2FA.
     */
    @Transactional
    public void disableTwoFactor(User user, int otp) {
        if (!verifyOtp(user.getTwoFactorSecret(), otp)) {
            throw new com.hotelchain.pro.common.exception.HotelChainException("2FA_INVALID", "OTP không hợp lệ");
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
    }
}
