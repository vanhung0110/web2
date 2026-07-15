package com.hotelchain.pro.repository;

import com.hotelchain.pro.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByRoomNumber(String roomNumber);
    List<Room> findByIsOccupied(Boolean isOccupied);
    boolean existsByRoomNumber(String roomNumber);
    long countByIsOccupiedTrue();
    long countByIsOccupiedFalse();
}
