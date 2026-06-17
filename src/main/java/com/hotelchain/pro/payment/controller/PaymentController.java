package com.hotelchain.pro.payment.controller;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.response.ApiResponse;
import com.hotelchain.pro.payment.dto.*;
import com.hotelchain.pro.payment.service.BankConfigService;
import com.hotelchain.pro.payment.service.PaymentService;
import com.hotelchain.pro.payment.service.PaymentSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Module 4 — Payment & QR Bank API.
 */
@Tag(name = "Payments", description = "Thanh toán & QR ngân hàng")
@Slf4j
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final BankConfigService bankConfigService;
    private final PaymentSecurityService paymentSecurityService;

    // ===== Bank Config Admin =====

    /** GET /api/v1/admin/bank-configs/{propertyId} — Xem cấu hình ngân hàng */
    @Operation(summary = "Xem cấu hình ngân hàng chi nhánh")
    @GetMapping("/api/v1/admin/bank-configs/{propertyId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<BankConfigDto>> getBankConfig(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.success(bankConfigService.getBankConfig(propertyId)));
    }

    /** PUT /api/v1/admin/bank-configs/{propertyId} — Cập nhật cấu hình ngân hàng */
    @Operation(summary = "Cập nhật cấu hình ngân hàng")
    @PutMapping("/api/v1/admin/bank-configs/{propertyId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<BankConfigDto>> updateBankConfig(
            @PathVariable UUID propertyId,
            @Valid @RequestBody UpdateBankConfigRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công",
                bankConfigService.updateBankConfig(propertyId, request, user)));
    }

    /** GET /api/v1/admin/bank-configs/{propertyId}/qr — Xem QR tĩnh */
    @Operation(summary = "Xem QR tĩnh của chi nhánh")
    @GetMapping("/api/v1/admin/bank-configs/{propertyId}/qr")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> getStaticQr(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.success(bankConfigService.getStaticQr(propertyId)));
    }

    /** POST /api/v1/admin/bank-configs/{propertyId}/qr/regenerate — Tạo lại QR tĩnh */
    @Operation(summary = "Tạo lại QR tĩnh")
    @PostMapping("/api/v1/admin/bank-configs/{propertyId}/qr/regenerate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> regenerateStaticQr(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.success("Tạo lại QR thành công",
                bankConfigService.regenerateStaticQr(propertyId)));
    }

    // ===== Payment Operations =====

    /** POST /api/v1/payments/generate-qr — Sinh QR động cho booking */
    @Operation(summary = "Sinh QR thanh toán động cho booking")
    @PostMapping("/api/v1/payments/generate-qr")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<QRPaymentResponse>> generateQr(
            @RequestBody GenerateBookingQrRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.generateQrForBooking(request)));
    }

    /** POST /api/v1/payments/confirm — Xác nhận thanh toán thủ công */
    @Operation(summary = "Xác nhận thanh toán thủ công")
    @PostMapping("/api/v1/payments/confirm")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Object>> confirmPayment(
            @RequestBody ConfirmPaymentRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thanh toán thành công",
                paymentService.confirmPayment(request, user)));
    }

    /** POST /api/v1/payments/webhook — Nhận webhook từ ngân hàng */
    @Operation(summary = "Nhận webhook thanh toán tự động từ ngân hàng")
    @PostMapping("/api/v1/payments/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "X-Bank-Code", required = false) String bankCode) {

        log.info("Payment webhook received from bank: {}", bankCode);

        // Verify signature nếu có
        if (signature != null) {
            // Lấy secret từ property config theo bankCode
            // Nếu signature không hợp lệ, reject
        }

        paymentService.processWebhook(payload);
        return ResponseEntity.ok("OK");
    }

    /** GET /api/v1/payments/booking/{bookingId} — Lịch sử thanh toán */
    @Operation(summary = "Lịch sử thanh toán của booking")
    @GetMapping("/api/v1/payments/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<Object>>> getPaymentHistory(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentHistory(bookingId)));
    }

    /** POST /api/v1/payments/refund — Hoàn tiền */
    @Operation(summary = "Hoàn tiền cho khách")
    @PostMapping("/api/v1/payments/refund")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Object>> refundPayment(
            @RequestBody RefundRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Hoàn tiền thành công",
                paymentService.refundPayment(request, user)));
    }
}
