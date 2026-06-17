package com.hotelchain.pro.staff.repository;

import com.hotelchain.pro.staff.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {
    List<Shift> findByPropertyIdAndScheduledStartBetween(UUID propertyId, LocalDateTime start, LocalDateTime end);
    List<Shift> findByScheduledStartBetween(LocalDateTime start, LocalDateTime end);
}
