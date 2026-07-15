package com.hotelchain.pro.repository;

import com.hotelchain.pro.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByUserIdAndIsActiveTrue(UUID userId);
    Optional<Tenant> findByRoomIdAndIsActiveTrue(UUID roomId);
    List<Tenant> findByIsActiveTrue();
    boolean existsByRoomIdAndIsActiveTrue(UUID roomId);
}
