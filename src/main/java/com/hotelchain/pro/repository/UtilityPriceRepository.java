package com.hotelchain.pro.repository;

import com.hotelchain.pro.entity.UtilityPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilityPriceRepository extends JpaRepository<UtilityPrice, UUID> {
    Optional<UtilityPrice> findFirstByOrderByEffectiveFromDesc();
}
