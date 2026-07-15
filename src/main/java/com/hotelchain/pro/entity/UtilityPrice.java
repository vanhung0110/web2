package com.hotelchain.pro.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "utility_prices")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UtilityPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal waterPricePerUnit;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal electricPricePerUnit;

    @Column(precision = 15, scale = 2)
    private BigDecimal internetFee = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal trashFee = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate effectiveFrom = LocalDate.now();

    @Column(length = 100)
    private String bankName;

    @Column(length = 100)
    private String bankAccount;

    @Column(length = 255)
    private String accountName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
