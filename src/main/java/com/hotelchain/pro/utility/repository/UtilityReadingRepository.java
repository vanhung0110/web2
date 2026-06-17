package com.hotelchain.pro.utility.repository;

import com.hotelchain.pro.utility.entity.UtilityReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilityReadingRepository extends JpaRepository<UtilityReading, UUID> {
    Optional<UtilityReading> findByBookingId(UUID bookingId);
    List<UtilityReading> findByRoomId(UUID roomId);
}
