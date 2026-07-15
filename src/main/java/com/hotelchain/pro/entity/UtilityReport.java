package com.hotelchain.pro.entity;

import com.hotelchain.pro.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "utility_reports")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UtilityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private Integer reportMonth;

    @Column(nullable = false)
    private Integer reportYear;

    // Chỉ số nước
    @Column(nullable = false)
    private Double waterOld = 0.0;

    @Column(nullable = false)
    private Double waterNew = 0.0;

    private String waterPhotoKey;

    // Chỉ số điện
    @Column(nullable = false)
    private Double electricOld = 0.0;

    @Column(nullable = false)
    private Double electricNew = 0.0;

    private String electricPhotoKey;

    // Lượng tiêu thụ (auto-calc)
    private Double waterUsage;
    private Double electricUsage;

    // Chi phí
    @Column(precision = 15, scale = 2)
    private BigDecimal waterCost;

    @Column(precision = 15, scale = 2)
    private BigDecimal electricCost;

    @Column(precision = 15, scale = 2)
    private BigDecimal roomRent;

    @Column(precision = 15, scale = 2)
    private BigDecimal internetFee = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal trashFee = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalCost;

    // Trạng thái duyệt
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rejectReason;

    // Người gửi / duyệt
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean isPaid = false;

    private LocalDateTime paymentDate;
}
