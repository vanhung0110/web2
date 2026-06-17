package com.hotelchain.pro.room.repository;

import com.hotelchain.pro.common.enums.RoomStatus;
import com.hotelchain.pro.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByPropertyId(UUID propertyId);

    List<Room> findByPropertyIdAndStatus(UUID propertyId, RoomStatus status);

    List<Room> findByPropertyIdAndDeletedFalseOrderByFloorAscRoomNumberAsc(UUID propertyId);

    /**
     * Tìm các phòng trống trong khoảng thời gian nhất định.
     * Phòng có status AVAILABLE và không có booking đang active trong khoảng thời gian.
     */
    @Query("""
            SELECT r FROM Room r
            WHERE r.property.id = :propertyId
            AND r.status = 'AVAILABLE'
            AND r.deleted = false
            AND r.id NOT IN (
                SELECT b.room.id FROM Booking b
                WHERE b.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECK_IN_READY')
                AND b.checkInPlan < :checkOut
                AND b.checkOutPlan > :checkIn
                AND b.deleted = false
            )
            """)
    List<Room> findAvailableRooms(
            @Param("propertyId") UUID propertyId,
            @Param("checkIn") LocalDateTime checkIn,
            @Param("checkOut") LocalDateTime checkOut
    );

    @Query("SELECT COUNT(r) FROM Room r WHERE r.property.id = :propertyId AND r.status = :status AND r.deleted = false")
    long countByPropertyIdAndStatus(@Param("propertyId") UUID propertyId, @Param("status") RoomStatus status);
}
