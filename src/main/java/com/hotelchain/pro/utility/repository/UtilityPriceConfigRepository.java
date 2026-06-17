package com.hotelchain.pro.utility.repository;

import com.hotelchain.pro.utility.entity.UtilityPriceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilityPriceConfigRepository extends JpaRepository<UtilityPriceConfig, UUID> {
    Optional<UtilityPriceConfig> findFirstByPropertyIdAndIsActiveTrueOrderByCreatedAtDesc(UUID propertyId);
}
