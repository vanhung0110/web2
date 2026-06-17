package com.hotelchain.pro.utility.entity;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.booking.entity.Booking;
import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.ReadingStatus;
import com.hotelchain.pro.room.entity.Room;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UtilityReading — Chỉ số đồng hồ nước / điện theo từng booking.
 * Mỗi Booking có 1 UtilityReading.
 * Đảm bảo minh bạch, có ảnh bằng chứng, chống tranh chấp.
 */
@Entity
@Table(name = "utility_readings", indexes = {
        @Index(name = "idx_utility_readings_booking", columnList = "booking_id"),
        @Index(name = "idx_utility_readings_room", columnList = "room_id")
})
@Getter
@Setter
public class UtilityReading extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // ===== ĐỒNG HỒ NƯỚC =====
    private Double waterIndexStart;         // Chỉ số nước đầu kỳ (check-in)
    private Double waterIndexEnd;           // Chỉ số nước cuối kỳ (check-out)
    private Double waterUsage;              // Lượng nước tiêu thụ (m³)

    @Column(precision = 15, scale = 2)
    private BigDecimal waterPricePerUnit;   // Giá nước / m³

    @Column(precision = 15, scale = 2)
    private BigDecimal waterTotal;          // Thành tiền nước

    // Ảnh đồng hồ nước (MinIO keys)
    private String waterPhotoStartKey;      // ảnh check-in
    private String waterPhotoEndKey;        // ảnh check-out
    private String waterPhotoStartHash;     // SHA-256 chống sửa ảnh
    private String waterPhotoEndHash;

    private Boolean waterPhotoStartVerified = false;
    private Boolean waterPhotoEndVerified = false;

    // Nhập tay
    private Double waterManualInputStart;
    private Double waterManualInputEnd;
    private Boolean waterManualVerified = false;    // Admin xác nhận

    @Column(columnDefinition = "TEXT")
    private String waterDiscrepancyNote;    // Ghi chú nếu chênh lệch ảnh vs nhập tay

    // ===== ĐỒNG HỒ ĐIỆN =====
    private Double electricIndexStart;
    private Double electricIndexEnd;
    private Double electricUsage;           // kWh tiêu thụ

    @Column(precision = 15, scale = 2)
    private BigDecimal electricPricePerUnit; // Giá điện / kWh (weighted average)

    @Column(precision = 15, scale = 2)
    private BigDecimal electricTotal;

    // Ảnh đồng hồ điện (MinIO keys)
    private String electricPhotoStartKey;
    private String electricPhotoEndKey;
    private String electricPhotoStartHash;
    private String electricPhotoEndHash;

    private Boolean electricPhotoStartVerified = false;
    private Boolean electricPhotoEndVerified = false;

    private Double electricManualInputStart;
    private Double electricManualInputEnd;
    private Boolean electricManualVerified = false;

    @Column(columnDefinition = "TEXT")
    private String electricDiscrepancyNote;

    // ===== AUDIT =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_start_id")
    private User recordedByStart;           // Người ghi đầu kỳ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_end_id")
    private User recordedByEnd;             // Người ghi cuối kỳ

    private LocalDateTime recordedAtStart;
    private LocalDateTime recordedAtEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReadingStatus status = ReadingStatus.PENDING_START;
}
