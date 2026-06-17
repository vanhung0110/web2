package com.hotelchain.pro.property.entity;

import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.PropertyType;
import com.hotelchain.pro.payment.entity.BankConfig;
import com.hotelchain.pro.room.entity.RoomType;
import com.hotelchain.pro.staff.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Property — Chi nhánh / Cơ sở lưu trú.
 * Mỗi Tenant có thể có nhiều Property.
 */
@Entity
@Table(name = "properties", indexes = {
        @Index(name = "idx_properties_tenant", columnList = "tenant_id"),
        @Index(name = "idx_properties_code", columnList = "code")
})
@Getter
@Setter
public class Property extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String name;            // "Nhà Nghỉ Hoa Mai - Quận 1"

    @Column(nullable = false, length = 50)
    private String code;            // "HM-Q1-001"

    @Column(nullable = false)
    private String address;

    private String ward;            // Phường
    private String district;        // Quận/Huyện
    private String city;            // Tỉnh/Thành phố

    private Double latitude;
    private Double longitude;

    @Column(length = 20)
    private String phone;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyType type = PropertyType.GUESTHOUSE;

    private Integer starRating;     // 1-5

    @Column(nullable = false)
    private Boolean isActive = true;

    // Hình ảnh lưu tham chiếu MinIO
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "property_image_keys", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "image_key")
    private List<String> imageKeys = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RoomType> roomTypes = new ArrayList<>();

    @OneToOne(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BankConfig bankConfig;

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    private List<Staff> staff = new ArrayList<>();
}
