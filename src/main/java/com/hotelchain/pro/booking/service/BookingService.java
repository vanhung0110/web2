package com.hotelchain.pro.booking.service;

import com.hotelchain.pro.booking.dto.CreateBookingRequest;
import com.hotelchain.pro.booking.entity.Booking;
import com.hotelchain.pro.booking.entity.BookingStatus;
import com.hotelchain.pro.booking.repository.BookingRepository;
import com.hotelchain.pro.entity.Room;
import com.hotelchain.pro.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Booking createBooking(CreateBookingRequest request) {
        if (request.getCheckOutDate().isBefore(request.getCheckInDate())) {
            throw new RuntimeException("Ngày Check-out không được trước ngày Check-in");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại"));

        if (room.getIsOccupied()) {
            throw new RuntimeException("Phòng đang có người sử dụng, không thể đặt");
        }

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setGuestName(request.getGuestName());
        booking.setGuestPhone(request.getGuestPhone());
        booking.setGuestIdentity(request.getGuestIdentity());
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        
        long days = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (days == 0) days = 1; // Same day check-in/out counts as 1 day
        
        BigDecimal dailyRent = room.getDailyRent() != null ? room.getDailyRent() : BigDecimal.ZERO;
        booking.setTotalPrice(dailyRent.multiply(BigDecimal.valueOf(days)));
        booking.setStatus(BookingStatus.PENDING);
        
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking checkIn(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại"));
        
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể Check-in cho booking đang chờ (PENDING)");
        }

        Room room = booking.getRoom();
        if (room.getIsOccupied()) {
            throw new RuntimeException("Phòng đang có khách khác sử dụng, không thể Check-in");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        room.setIsOccupied(true);
        roomRepository.save(room);

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking checkOut(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại"));
        
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new RuntimeException("Booking phải ở trạng thái CHECKED_IN mới có thể Check-out");
        }

        booking.setStatus(BookingStatus.CHECKED_OUT);
        
        Room room = booking.getRoom();
        room.setIsOccupied(false);
        roomRepository.save(room);

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại"));
        
        if (booking.getStatus() == BookingStatus.CHECKED_OUT) {
            throw new RuntimeException("Booking đã trả phòng, không thể hủy");
        }

        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            Room room = booking.getRoom();
            room.setIsOccupied(false);
            roomRepository.save(room);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }
}
