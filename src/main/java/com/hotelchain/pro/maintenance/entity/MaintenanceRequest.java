package com.hotelchain.pro.maintenance.entity;

import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.MaintenancePriority;
import com.hotelchain.pro.common.enums.MaintenanceStatus;
import com.hotelchain.pro.property.entity.Property;
import com.hotelchain.pro.room.entity.Room;
import com.hotelchain.pro.staff.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * MaintenanceRequest — Yêu cầu bảo trì / sửa chữa phòng.
 */
@Entity
@Table(name = "maintenance_requests", indexes = {
        @Index(name = "idx_maintenance_property", columnList = "property_id"),
        @Index(name = "idx_maintenance_room", columnList = "room_id"),
        @Index(name = "idx_maintenance_status", columnList = "status"),
        @Index(name = "idx_maintenance_assigned", columnList = "assigned_to_id")
})
@Getter
@Setter
public class MaintenanceRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id")
    private Staff reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private Staff assignedTo;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenancePriority priority = MaintenancePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status = MaintenanceStatus.REPORTED;

    private LocalDateTime reportedAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Column(precision = 15, scale = 2)
    private BigDecimal repairCost;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "maintenance_before_photos", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "photo_key")
    private List<String> beforePhotoKeys = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "maintenance_after_photos", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "photo_key")
    private List<String> afterPhotoKeys = new ArrayList<>();
}
