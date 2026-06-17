package com.hotelchain.pro.payment.entity;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.BankCode;
import com.hotelchain.pro.property.entity.Property;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BankConfig — Cấu hình tài khoản ngân hàng nhận tiền cho từng chi nhánh.
 * Mỗi Property có 1 BankConfig. Dùng để sinh QR VietQR.
 */
@Entity
@Table(name = "bank_configs")
@Getter
@Setter
public class BankConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", unique = true, nullable = false)
    private Property property;

    // ===== THÔNG TIN TÀI KHOẢN NGÂN HÀNG =====
    @Column(nullable = false)
    private String accountHolderName;   // Tên chủ tài khoản (phải khớp chính xác)

    @Column(nullable = false)
    private String accountNumber;       // Số tài khoản (được mã hóa AES)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankCode bankCode;          // VCB, TCB, MB, BIDV...

    private String bankName;            // Tên hiển thị ngân hàng (từ BankCode.displayName)
    private String bankBin;             // BIN code cho VietQR (từ BankCode.bin)
    private String branch;              // Chi nhánh ngân hàng (tuỳ chọn)

    // ===== QR CONFIGURATION =====
    @Column(nullable = false)
    private Boolean isActive = true;

    private String templateDescription; // Nội dung chuyển khoản mặc định
    // VD: "THANH TOAN PHONG {BOOKING_CODE}"

    private Boolean requireUtilityPhoto = true;  // Bắt buộc chụp ảnh đồng hồ
    private Boolean requireUtilityInput = true;  // Bắt buộc nhập chỉ số tay

    // QR tĩnh cho property (cache)
    private String staticQrImageKey;    // MinIO key của QR tĩnh
    private LocalDateTime qrGeneratedAt;

    // Xác nhận thanh toán
    private Boolean autoConfirmEnabled = false;  // Xác nhận tự động qua webhook
    private String webhookSecret;               // Bí mật webhook ngân hàng (AES encrypted)
    private Integer confirmTimeoutMinutes = 30;  // Timeout xác nhận thủ công

    // Audit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "configured_by_id")
    private User configuredBy;

    private LocalDateTime lastUpdated;
}
