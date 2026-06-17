package com.hotelchain.pro.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QRPaymentResponse {
    private boolean success;
    private String qrCode;      // Base64 image
    private String qrDataUrl;   // data:image/png;base64,...
    private String qrImageKey;  // MinIO key (optional)
}
