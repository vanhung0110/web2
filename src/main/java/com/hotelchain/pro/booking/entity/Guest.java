package com.hotelchain.pro.booking.entity;

import com.hotelchain.pro.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Guest — Khách hàng.
 * Một Guest có thể có nhiều Booking trong lịch sử.
 */
@Entity
@Table(name = "guests", indexes = {
        @Index(name = "idx_guests_phone", columnList = "phone"),
        @Index(name = "idx_guests_id_number", columnList = "id_number"),
        @Index(name = "idx_guests_email", columnList = "email")
})
@Getter
@Setter
public class Guest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    // Thông tin giấy tờ tùy thân
    @Column(name = "id_number", length = 30)
    private String idNumber;           // CCCD/CMND (được mã hóa AES)

    @Column(length = 20)
    private String idType;             // "CCCD", "CMND", "PASSPORT"

    private String idImageFrontKey;    // MinIO key ảnh mặt trước
    private String idImageBackKey;     // MinIO key ảnh mặt sau

    private String nationality = "VN"; // Quốc tịch
    private String address;

    // Loyalty
    @OneToOne(mappedBy = "guest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private com.hotelchain.pro.loyalty.entity.LoyaltyAccount loyaltyAccount;

    @OneToMany(mappedBy = "guest", fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();
}
