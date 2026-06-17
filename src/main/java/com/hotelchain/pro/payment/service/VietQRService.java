package com.hotelchain.pro.payment.service;

import com.hotelchain.pro.payment.dto.GenerateQRRequest;
import com.hotelchain.pro.payment.dto.QRPaymentResponse;
import com.hotelchain.pro.payment.entity.BankConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * VietQR Integration Service — sinh QR động và tĩnh theo chuẩn Napas/VietQR.
 *
 * API: https://api.vietqr.io/v2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VietQRService {

    private static final String VIETQR_API = "https://api.vietqr.io/v2";

    @Value("${payment.vietqr.client-id:}")
    private String clientId;

    @Value("${payment.vietqr.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final com.hotelchain.pro.storage.service.MinioStorageService minioStorageService;

    /**
     * Sinh QR động cho từng lần thanh toán (có số tiền cụ thể).
     * Tích hợp VietQR API chuẩn Napas.
     */
    public QRPaymentResponse generateDynamicQR(GenerateQRRequest request) {
        try {
            String url = VIETQR_API + "/generate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("accountNo", request.getAccountNo());
            body.put("accountName", request.getAccountName());
            body.put("acqId", request.getBankBin());    // BIN ngân hàng
            body.put("addInfo", request.getTransferContent());
            body.put("amount", request.getAmount());
            body.put("template", "compact2");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String code = (String) responseBody.get("code");

                if ("00".equals(code)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    return QRPaymentResponse.builder()
                            .success(true)
                            .qrCode((String) data.get("qrCode"))
                            .qrDataUrl((String) data.get("qrDataURL"))
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("VietQR API error: {}", e.getMessage());
            // Fallback: sinh QR bằng ZXing cục bộ
            return generateLocalQR(request);
        }

        return QRPaymentResponse.builder().success(false).build();
    }

    /**
     * Sinh QR tĩnh (không có số tiền, dùng cache).
     * Tái sử dụng nhiều lần, khách tự nhập số tiền.
     */
    public String generateStaticQR(BankConfig bankConfig) {
        GenerateQRRequest request = GenerateQRRequest.builder()
                .accountNo(bankConfig.getAccountNumber())
                .accountName(bankConfig.getAccountHolderName())
                .bankBin(bankConfig.getBankBin())
                .transferContent(bankConfig.getTemplateDescription())
                .amount(0L) // Static QR không có số tiền
                .build();

        QRPaymentResponse response = generateDynamicQR(request);
        if (response.isSuccess() && response.getQrCode() != null) {
            // Save to MinIO
            byte[] qrBytes = java.util.Base64.getDecoder().decode(response.getQrCode());
            return saveQRImage(qrBytes, "static-" + bankConfig.getProperty().getCode());
        }
        return null;
    }

    /**
     * Lưu QR image vào MinIO, trả về object key.
     */
    public String saveQRImage(byte[] qrImageBytes, String name) {
        try {
            String objectKey = "qr/" + name + "-" + System.currentTimeMillis() + ".png";
            minioStorageService.uploadBytes(objectKey, qrImageBytes, "image/png");
            return objectKey;
        } catch (Exception e) {
            log.error("Error saving QR image to MinIO: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fallback: sinh QR cục bộ bằng ZXing khi VietQR API không khả dụng.
     */
    private QRPaymentResponse generateLocalQR(GenerateQRRequest request) {
        try {
            // Tạo QR content theo chuẩn VietQR EMV
            String qrContent = buildVietQRContent(request);

            com.google.zxing.MultiFormatWriter writer = new com.google.zxing.MultiFormatWriter();
            com.google.zxing.common.BitMatrix matrix = writer.encode(
                    qrContent,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    400, 400
            );

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            byte[] qrBytes = outputStream.toByteArray();

            String base64 = java.util.Base64.getEncoder().encodeToString(qrBytes);
            return QRPaymentResponse.builder()
                    .success(true)
                    .qrCode(base64)
                    .qrDataUrl("data:image/png;base64," + base64)
                    .build();
        } catch (Exception e) {
            log.error("Local QR generation error: {}", e.getMessage());
            return QRPaymentResponse.builder().success(false).build();
        }
    }

    private String buildVietQRContent(GenerateQRRequest request) {
        // Chuẩn VietQR EMV QRCPS format
        return String.format(
                "00020101021238%s%s%s%s5303704540%s5802VN6304",
                request.getBankBin() != null ? request.getBankBin() : "",
                request.getAccountNo() != null ? request.getAccountNo() : "",
                request.getAccountName() != null ? request.getAccountName() : "",
                request.getTransferContent() != null ? request.getTransferContent() : "",
                request.getAmount() > 0 ? request.getAmount() : ""
        );
    }
}
