package com.hotelchain.pro.maintenance.repository;

import com.hotelchain.pro.maintenance.entity.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {
    List<MaintenanceRequest> findByPropertyId(UUID propertyId);
    List<MaintenanceRequest> findByRoomId(UUID roomId);
}
