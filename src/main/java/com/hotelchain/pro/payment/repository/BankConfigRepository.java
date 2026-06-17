package com.hotelchain.pro.payment.repository;

import com.hotelchain.pro.payment.entity.BankConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankConfigRepository extends JpaRepository<BankConfig, UUID> {
    Optional<BankConfig> findByPropertyId(UUID propertyId);
}
