package com.hotelchain.pro.report.service;

import com.hotelchain.pro.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Dynamic Pricing Service — tự động điều chỉnh giá phòng theo:
 * - Tỷ lệ lấp đầy hiện tại
 * - Mùa cao điểm / thấp điểm
 * - Ngày lễ, sự kiện đặc biệt
 * - Weekend surge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPricingService {

    @Value("${pricing.weekend-surge-rate:1.20}")
    private BigDecimal weekendSurgeRate;

    @Value("${pricing.high-occupancy-threshold:0.80}")
    private double highOccupancyThreshold;

    @Value("${pricing.high-occupancy-surge-rate:1.15}")
    private BigDecimal highOccupancySurgeRate;

    private final com.hotelchain.pro.room.repository.RoomTypeRepository roomTypeRepository;

    /**
     * Tính giá tối ưu cho một ngày cụ thể.
     */
    public BigDecimal calculateOptimalPrice(
            UUID roomTypeId,
            LocalDate date,
            DayOfWeek dayOfWeek,
            Double currentOccupancyRate) {

        BigDecimal basePrice = roomTypeRepository.findById(roomTypeId)
                .map(rt -> rt.getBasePrice())
                .orElse(BigDecimal.ZERO);

        if (basePrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal multiplier = BigDecimal.ONE;

        // Weekend surge: +20%
        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) {
            multiplier = multiplier.multiply(weekendSurgeRate);
            log.debug("Weekend surge applied: {}x", weekendSurgeRate);
        }

        // High occupancy surge (>80%): +15%
        if (currentOccupancyRate != null && currentOccupancyRate > highOccupancyThreshold) {
            multiplier = multiplier.multiply(highOccupancySurgeRate);
            log.debug("High occupancy surge applied: {}x (occupancy: {}%)",
                    highOccupancySurgeRate, currentOccupancyRate * 100);
        }

        // Holiday pricing (tuỳ chỉnh trong database)
        // holidayService.getPriceMultiplier(date) — TODO: implement holiday config

        BigDecimal finalPrice = basePrice.multiply(multiplier)
                .setScale(0, RoundingMode.HALF_UP);

        log.debug("Dynamic price for roomType {} on {}: {} (base: {}, multiplier: {})",
                roomTypeId, date, finalPrice, basePrice, multiplier);

        return finalPrice;
    }

    /**
     * Tính giá cho nhiều ngày (dùng khi tạo booking).
     */
    public BigDecimal calculateTotalRoomFee(
            UUID roomTypeId,
            LocalDate checkIn,
            LocalDate checkOut,
            Double currentOccupancyRate) {

        BigDecimal total = BigDecimal.ZERO;
        LocalDate date = checkIn;

        while (date.isBefore(checkOut)) {
            BigDecimal dailyPrice = calculateOptimalPrice(
                    roomTypeId, date, date.getDayOfWeek(), currentOccupancyRate);
            total = total.add(dailyPrice);
            date = date.plusDays(1);
        }

        return total;
    }
}
