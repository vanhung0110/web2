package com.hotelchain.pro.notification.entity;

import com.hotelchain.pro.common.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification — Thông báo gửi đến người dùng.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_notifications_unread", columnList = "user_id, is_read")
})
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String data;            // JSON payload

    @Column(nullable = false)
    private Boolean isRead = false;

    private LocalDateTime readAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Reference to related entity
    private String entityType;      // "Booking", "Payment"
    private UUID entityId;

    // Channels sent
    private Boolean sentViaEmail = false;
    private Boolean sentViaSms = false;
    private Boolean sentViaPush = false;
    private Boolean sentViaZalo = false;
}
