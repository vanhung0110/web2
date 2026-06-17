package com.hotelchain.pro.staff.entity;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.Role;
import com.hotelchain.pro.property.entity.Property;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Staff — Nhân viên làm việc tại một chi nhánh.
 * Liên kết với User account để đăng nhập.
 */
@Entity
@Table(name = "staff", indexes = {
        @Index(name = "idx_staff_property", columnList = "property_id"),
        @Index(name = "idx_staff_user", columnList = "user_id")
})
@Getter
@Setter
public class Staff extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private String fullName;

    @Column(length = 20)
    private String phone;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;              // RECEPTIONIST, ACCOUNTANT, HOUSEKEEPING, MAINTENANCE

    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDate startDate;    // Ngày bắt đầu làm việc
    private LocalDate endDate;      // Ngày nghỉ việc

    private String avatarKey;       // MinIO key avatar

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "staff", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Shift> shifts = new ArrayList<>();
}
