package com.hotelchain.pro.property.entity;

import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tenant — Chủ sở hữu / Doanh nghiệp sở hữu chuỗi khách sạn.
 */
@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenants_code", columnList = "code", unique = true)
})
@Getter
@Setter
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;            // "HOTELCHAIN_001"

    @Column(nullable = false)
    private String businessName;    // Tên doanh nghiệp

    @Column(length = 20)
    private String taxCode;         // Mã số thuế

    @Column(length = 50)
    private String licenseNumber;   // Giấy phép kinh doanh

    @Column(nullable = false)
    private String contactEmail;

    @Column(length = 20)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.TRIAL;

    private LocalDate subscriptionExpiry;

    @Column(nullable = false)
    private Integer maxProperties = 5; // Giới hạn số cơ sở

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Property> properties = new ArrayList<>();
}
