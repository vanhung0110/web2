package com.hotelchain.pro.report.service;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.exception.ResourceNotFoundException;
import com.hotelchain.pro.property.entity.Property;
import com.hotelchain.pro.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PropertyRepository propertyRepository;

    public List<UUID> getActivePropertyIds() {
        return propertyRepository.findAllActivePropertyIds();
    }

    public Object getChainDashboard(User user) {
        List<Property> properties = propertyRepository.findAll();
        long totalProperties = properties.size();
        long activeProperties = properties.stream().filter(Property::getIsActive).count();

        return Map.of(
                "totalProperties", totalProperties,
                "activeProperties", activeProperties,
                "chainRevenueToday", 0L,
                "averageOccupancyRate", 0.0,
                "activePropertiesDetails", properties.stream().map(p -> Map.of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "code", p.getCode(),
                        "city", p.getCity() != null ? p.getCity() : ""
                )).collect(Collectors.toList())
        );
    }

    public Object getPropertyDashboard(UUID propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId.toString()));

        return Map.of(
                "propertyId", property.getId(),
                "propertyName", property.getName(),
                "today", Map.of(
                        "checkIns", 0,
                        "checkOuts", 0,
                        "newBookings", 0,
                        "revenue", 0L
                ),
                "occupancy", Map.of(
                        "totalRooms", 0,
                        "occupiedRooms", 0,
                        "availableRooms", 0,
                        "maintenanceRooms", 0,
                        "occupancyRate", 0.0
                ),
                "revenue", Map.of(
                        "today", 0L,
                        "thisWeek", 0L,
                        "thisMonth", 0L,
                        "trend", "0.0%"
                ),
                "pendingActions", Map.of(
                        "checkInsToday", 0,
                        "checkOutsToday", 0,
                        "pendingPayments", 0,
                        "roomsNeedCleaning", 0
                )
        );
    }
}
