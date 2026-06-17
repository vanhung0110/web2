package com.hotelchain.pro.booking.repository;

import com.hotelchain.pro.booking.entity.Booking;
import com.hotelchain.pro.common.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingCode(String bookingCode);

    Page<Booking> findByPropertyIdAndDeletedFalseOrderByCreatedAtDesc(UUID propertyId, Pageable pageable);

    @Query("""
            SELECT b FROM Booking b
            WHERE (:propertyId IS NULL OR b.propertyId = :propertyId)
            AND (:status IS NULL OR b.status = :status)
            AND (:searchQuery IS NULL OR LOWER(b.guest.fullName) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR LOWER(b.bookingCode) LIKE LOWER(CONCAT('%', :searchQuery, '%')))
            AND (:fromTime IS NULL OR b.checkInPlan >= :fromTime)
            AND (:toTime IS NULL OR b.checkOutPlan <= :toTime)
            AND b.deleted = false
            """)
    Page<Booking> findByFilters(
            @Param("propertyId") UUID propertyId,
            @Param("status") BookingStatus status,
            @Param("searchQuery") String searchQuery,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            Pageable pageable
    );

    List<Booking> findByPropertyIdAndStatusIn(UUID propertyId, List<BookingStatus> statuses);

    /**
     * Kiểm tra xung đột đặt phòng — phòng đã được đặt trong khoảng thời gian này chưa?
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.room.id = :roomId
            AND b.status IN ('PENDING', 'CONFIRMED', 'CHECK_IN_READY', 'CHECKED_IN')
            AND b.checkInPlan < :checkOut
            AND b.checkOutPlan > :checkIn
            AND b.deleted = false
            AND (:excludeBookingId IS NULL OR b.id != :excludeBookingId)
            """)
    long countConflictingBookings(
            @Param("roomId") UUID roomId,
            @Param("checkIn") LocalDateTime checkIn,
            @Param("checkOut") LocalDateTime checkOut,
            @Param("excludeBookingId") UUID excludeBookingId
    );

    @Query("""
            SELECT b FROM Booking b
            WHERE b.propertyId = :propertyId
            AND DATE(b.checkOutPlan) = CURRENT_DATE
            AND b.status = 'CHECKED_IN'
            """)
    List<Booking> findTodayCheckouts(@Param("propertyId") UUID propertyId);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.propertyId = :propertyId
            AND DATE(b.checkInPlan) = CURRENT_DATE
            AND b.status IN ('CONFIRMED', 'CHECK_IN_READY')
            """)
    List<Booking> findTodayCheckIns(@Param("propertyId") UUID propertyId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.propertyId = :propertyId AND b.status = 'CHECKED_IN' AND b.deleted = false")
    long countOccupiedRooms(@Param("propertyId") UUID propertyId);

    // Tìm booking pending quá 15 phút để auto-cancel
    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = 'PENDING'
            AND b.createdAt < :cutoffTime
            AND b.deleted = false
            """)
    List<Booking> findExpiredPendingBookings(@Param("cutoffTime") LocalDateTime cutoffTime);
}
