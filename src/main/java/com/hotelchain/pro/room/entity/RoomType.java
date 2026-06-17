package com.hotelchain.pro.room.entity;

import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.property.entity.Property;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * RoomType — Loại phòng (VD: Phòng đơn, Phòng đôi, Suite).
 * Mỗi Property có nhiều RoomType.
 */
@Entity
@Table(name = "room_types", indexes = {
        @Index(name = "idx_room_types_property", columnList = "property_id")
})
@Getter
@Setter
public class RoomType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private String name;            // "Phòng Đơn", "Phòng Đôi", "Suite"

    @Column(nullable = false)
    private String code;            // "SINGLE", "DOUBLE", "SUITE"

    @Column(columnDefinition = "TEXT")
    private String description;

    // Giá phòng
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal basePrice;   // Giá cơ bản / đêm

    private Integer maxOccupancy;   // Số người tối đa
    private Integer bedCount;       // Số giường
    private String bedType;         // "SINGLE", "DOUBLE", "KING", "TWIN"
    private Double area;            // Diện tích m²

    private String amenities;       // JSON list tiện nghi ["WiFi", "AC", "TV", "Fridge"]

    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer totalRooms = 0; // Tổng số phòng loại này
}
