package com.hotelchain.pro.staff.repository;

import com.hotelchain.pro.staff.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {
    Page<Staff> findByPropertyId(UUID propertyId, Pageable pageable);
    Optional<Staff> findByUserId(UUID userId);
}
