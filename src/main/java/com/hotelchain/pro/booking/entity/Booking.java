package com.hotelchain.pro.booking.entity;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.BookingSource;
import com.hotelchain.pro.common.enums.BookingStatus;
import com.hotelchain.pro.payment.entity.Payment;
import com.hotelchain.pro.room.entity.Room;
import com.hotelchain.pro.utility.entity.UtilityReading;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Booking — Đặt phòng.
 * Trung tâm của hệ thống, kết nối Room, Guest, Payment, UtilityReading.
 */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_room_dates", columnList = "room_id, check_in_plan, check_out_plan"),
        @Index(name = "idx_bookings_status", columnList = "status"),
        @Index(name = "idx_bookings_property", columnList = "property_id, created_at"),
        @Index(name = "idx_bookings_code", columnList = "booking_code", unique = true),
        @Index(name = "idx_bookings_guest", columnList = "guest_id")
})
@Getter
@Setter
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String bookingCode;     // "BK-20250615-0001"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;     // Staff tạo booking

    // Tham chiếu property_id để query nhanh (denormalized)
    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    // Thời gian
    @Column(nullable = false)
    private LocalDateTime checkInPlan;

    @Column(nullable = false)
    private LocalDateTime checkOutPlan;

    private LocalDateTime actualCheckIn;
    private LocalDateTime actualCheckOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private BookingSource source = BookingSource.WALK_IN;

    private Integer adultsCount = 1;
    private Integer childrenCount = 0;

    // ===== GIÁ PHÒNG =====
    @Column(precision = 15, scale = 2)
    private BigDecimal roomRatePerNight;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalRoomFee;

    @Column(precision = 15, scale = 2)
    private BigDecimal utilityCost = BigDecimal.ZERO;    // Tiền điện nước

    @Column(precision = 15, scale = 2)
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal remainingAmount;

    // Ghi chú
    @Column(columnDefinition = "TEXT")
    private String specialRequests;

    @Column(columnDefinition = "TEXT")
    private String internalNote;

    // Self check-in token
    private String selfCheckInToken;
    private LocalDateTime selfCheckInTokenExpiry;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UtilityReading utilityReading;
}
