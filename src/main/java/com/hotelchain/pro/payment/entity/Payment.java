package com.hotelchain.pro.payment.entity;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.booking.entity.Booking;
import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.PaymentMethod;
import com.hotelchain.pro.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment — Giao dịch thanh toán.
 * Một Booking có thể có nhiều Payment (đặt cọc, thanh toán cuối, hoàn tiền).
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_booking", columnList = "booking_id"),
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_transaction_id", columnList = "transaction_id")
})
@Getter
@Setter
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    // QR/Transaction info
    private String transactionId;    // ID giao dịch từ ngân hàng
    private String qrCode;           // QR code data (URL hoặc base64)
    private String qrImageKey;       // MinIO key của QR image

    // VietQR response
    private String vietQrCode;
    private String vietQrDataUrl;

    // Nội dung chuyển khoản
    private String transferContent;  // "THANH TOAN PHONG BK-20250615-0001"

    // Xác nhận
    private LocalDateTime paidAt;
    private LocalDateTime confirmedAt;
    private Boolean autoConfirmed = false; // Xác nhận tự động qua webhook

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_id")
    private User confirmedBy;        // Nhân viên xác nhận thủ công

    // Hoàn tiền
    @Column(precision = 15, scale = 2)
    private BigDecimal refundAmount;
    private LocalDateTime refundedAt;
    private String refundReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Idempotency
    private String idempotencyKey;   // Chống xử lý webhook trùng
}
