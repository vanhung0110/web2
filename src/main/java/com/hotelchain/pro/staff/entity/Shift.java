package com.hotelchain.pro.staff.entity;

import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.common.enums.ShiftType;
import com.hotelchain.pro.property.entity.Property;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shift — Ca làm việc của nhân viên.
 * Hỗ trợ chấm công vào/ra, GPS clock-in.
 */
@Entity
@Table(name = "shifts", indexes = {
        @Index(name = "idx_shifts_staff", columnList = "staff_id"),
        @Index(name = "idx_shifts_property_date", columnList = "property_id, scheduled_start")
})
@Getter
@Setter
public class Shift extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private LocalDateTime scheduledStart;

    @Column(nullable = false)
    private LocalDateTime scheduledEnd;

    private LocalDateTime actualStart;      // Thực tế clock-in
    private LocalDateTime actualEnd;        // Thực tế clock-out

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftType type;                 // MORNING, AFTERNOON, NIGHT, FULL_DAY

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Boolean isOvertime = false;

    // Chấm công bằng GPS (tùy chọn)
    private Double clockInLatitude;
    private Double clockInLongitude;
    private Double clockOutLatitude;
    private Double clockOutLongitude;
}
