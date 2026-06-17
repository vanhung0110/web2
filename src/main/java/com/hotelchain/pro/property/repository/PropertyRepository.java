package com.hotelchain.pro.property.repository;

import com.hotelchain.pro.property.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {

    List<Property> findByTenantId(UUID tenantId);

    List<Property> findByTenantIdAndIsActiveTrue(UUID tenantId);

    Optional<Property> findByCode(String code);

    boolean existsByCodeAndDeletedFalse(String code);

    @Query("SELECT p.id FROM Property p WHERE p.isActive = true AND p.deleted = false")
    List<UUID> findAllActivePropertyIds();
}
