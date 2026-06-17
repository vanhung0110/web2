package com.hotelchain.pro.booking.repository;

import com.hotelchain.pro.booking.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestRepository extends JpaRepository<Guest, UUID> {
    Optional<Guest> findByPhone(String phone);
    Optional<Guest> findByIdNumber(String idNumber);
}
