package com.hotelchain.pro.security.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwoFactorSetupResponse {
    private String secret;       // Base32 encoded secret key
    private String qrCodeUri;   // otpauth:// URI để tạo QR code
    private String issuer;
}
