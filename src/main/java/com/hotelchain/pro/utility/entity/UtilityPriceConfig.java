package com.hotelchain.pro.utility.entity;

import com.hotelchain.pro.common.entity.BaseEntity;
import com.hotelchain.pro.property.entity.Property;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * UtilityPriceConfig — Cấu hình giá điện / nước theo từng chi nhánh.
 * Giá điện theo bậc thang (EVN), nước theo giá cố định.
 */
@Entity
@Table(name = "utility_price_configs", indexes = {
        @Index(name = "idx_utility_price_property", columnList = "property_id")
})
@Getter
@Setter
public class UtilityPriceConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    // Giá nước (đồng/m³)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal waterPricePerUnit = new BigDecimal("15000");

    // Giá điện theo bậc thang (theo quy định EVN)
    @Column(name = "electric_tier1_price", precision = 15, scale = 2)
    private BigDecimal electricTier1Price = new BigDecimal("1728");  // 0-50 kWh
    @Column(name = "electric_tier2_price", precision = 15, scale = 2)
    private BigDecimal electricTier2Price = new BigDecimal("1786");  // 51-100 kWh
    @Column(name = "electric_tier3_price", precision = 15, scale = 2)
    private BigDecimal electricTier3Price = new BigDecimal("2074");  // 101-200 kWh
    @Column(name = "electric_tier4_price", precision = 15, scale = 2)
    private BigDecimal electricTier4Price = new BigDecimal("2612");  // 201-300 kWh
    @Column(name = "electric_tier5_price", precision = 15, scale = 2)
    private BigDecimal electricTier5Price = new BigDecimal("2919");  // 301-400 kWh
    @Column(name = "electric_tier6_price", precision = 15, scale = 2)
    private BigDecimal electricTier6Price = new BigDecimal("3015");  // >400 kWh

    // Hoặc giá cố định (dành cho nhà nghỉ nhỏ)
    private Boolean useFixedElectricPrice = true;

    @Column(precision = 15, scale = 2)
    private BigDecimal fixedElectricPrice = new BigDecimal("3500"); // Giá điện cố định

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private Boolean isActive = true;
}
