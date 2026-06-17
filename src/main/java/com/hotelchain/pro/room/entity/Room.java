package com.hotelchain.pro.room.entity;

import com.hotelchain.pro.booking.entity.Booking;
import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.RoomStatus;
import com.hotelchain.pro.common.enums.RoomView;
import com.hotelchain.pro.property.entity.Property;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Room — Phòng cụ thể trong một chi nhánh.
 * Mỗi RoomType có nhiều Room.
 */
@Entity
@Table(name = "rooms", indexes = {
        @Index(name = "idx_rooms_property", columnList = "property_id"),
        @Index(name = "idx_rooms_room_type", columnList = "room_type_id"),
        @Index(name = "idx_rooms_status", columnList = "status")
})
@Getter
@Setter
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(nullable = false, length = 20)
    private String roomNumber;      // "101", "A02"

    @Column(nullable = false)
    private Integer floor;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.AVAILABLE;

    // Chỉ số tiện ích ban đầu (khi bàn giao phòng)
    private Double initialWaterIndex = 0.0;
    private Double initialElectricIndex = 0.0;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "room_image_keys", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "image_key")
    private List<String> imageKeys = new ArrayList<>();

    private Boolean hasBalcony = false;
    private Boolean hasWindow = true;

    @Enumerated(EnumType.STRING)
    private RoomView viewType;      // STREET, GARDEN, SEA, CITY

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();
}
