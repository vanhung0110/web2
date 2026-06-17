package com.hotelchain.pro.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AuditLog — Ghi lại mọi hành động quan trọng trong hệ thống.
 * Giữ nguyên 90 ngày, sau đó archive.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_entity", columnList = "entity_type, entity_id, timestamp DESC"),
        @Index(name = "idx_audit_logs_user", columnList = "user_id, timestamp DESC"),
        @Index(name = "idx_audit_logs_tenant", columnList = "tenant_id, timestamp DESC"),
        @Index(name = "idx_audit_logs_timestamp", columnList = "timestamp DESC")
})
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
    @SequenceGenerator(name = "audit_log_seq", sequenceName = "audit_log_seq", allocationSize = 50)
    private Long id;

    private UUID userId;
    private String username;

    @Column(nullable = false, length = 100)
    private String action;          // "BOOKING_CREATED", "PAYMENT_CONFIRMED"

    @Column(nullable = false, length = 100)
    private String entityType;      // "Booking", "Payment"

    private UUID entityId;

    @Column(columnDefinition = "TEXT")
    private String oldValue;        // JSON snapshot trước

    @Column(columnDefinition = "TEXT")
    private String newValue;        // JSON snapshot sau

    @Column(length = 45)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @Column(length = 500)
    private String requestUri;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    private UUID tenantId;
    private UUID propertyId;
}
