package com.hotelchain.pro.service;

import com.hotelchain.pro.dto.CreateBookingRequest;
import com.hotelchain.pro.dto.CreateTenantRequest;
import com.hotelchain.pro.entity.Booking;
import com.hotelchain.pro.entity.Room;
import com.hotelchain.pro.entity.Tenant;
import com.hotelchain.pro.enums.BookingStatus;
import com.hotelchain.pro.repository.BookingRepository;
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
    private final TenantService tenantService;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Booking createBooking(CreateBookingRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("KhA'ng tAm thy phAng"));

        if (room.getIsOccupied()) {
            throw new RuntimeException("PhAng `A cA3 ng?i ");
        }

        // TAnnh ti`n tm th?i (s ngAy * giA dailyRent)
        long days = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (days <= 0) days = 1;
        Double dailyRent = room.getDailyRent() != null ? room.getDailyRent().doubleValue() : 0.0;
        BigDecimal totalPrice = BigDecimal.valueOf(days * dailyRent);

        Booking booking = Booking.builder()
                .room(room)
                .guestName(request.getGuestName())
                .guestPhone(request.getGuestPhone())
                .guestIdentity(request.getGuestIdentity())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalPrice(totalPrice)
                .status(BookingStatus.PENDING)
                .build();

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking checkIn(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KhA'ng tAm thy booking"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Booking nAy khA'ng cAAn ` trng thAi ch? nhn phAng");
        }

        Room room = booking.getRoom();
        if (room.getIsOccupied()) {
            throw new RuntimeException("PhAng `A cA3 ng?i , khA'ng th` check-in!");
        }

        // Cp nht Booking
        booking.setStatus(BookingStatus.CHECKED_IN);
        bookingRepository.save(booking);

        // To Tenant t `ng (chuy`n khAch cc thAnh ng?i thuA)
        CreateTenantRequest tenantReq = new CreateTenantRequest();
        tenantReq.setRoomId(room.getId());
        tenantReq.setFullName(booking.getGuestName());
        
        // N`u khA'ng cA3 s` `in thoi thA phAi phAt sinh s` o ` dng nhp
        String phone = booking.getGuestPhone();
        if (phone == null || phone.trim().isEmpty()) {
            phone = "000" + System.currentTimeMillis();
        }
        tenantReq.setPhone(phone);

        tenantService.createTenant(tenantReq);

        return booking;
    }

    @Transactional
    public Booking checkOut(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KhA'ng tAm thy booking"));

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new RuntimeException("Ch? booking `ang  m>i cA3 th` check-out");
        }

        booking.setStatus(BookingStatus.CHECKED_OUT);
        
        // GiAi phA3ng phAng
        Room room = booking.getRoom();
        room.setIsOccupied(false);
        roomRepository.save(room);

        // CA3 th` vA' hiu hA3a Tenant n`u c`n, nhng th?ng admin s t checkout tenant
        // Trong context Nha Tro thA checkout booking nAn kA"o theo remove tenant khA'ng?
        //  `Ay b qua x lA Tenant vA tenant s do admin quAn lA riAng!
        
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("KhA'ng tAm thy booking"));

        if (booking.getStatus() == BookingStatus.CHECKED_OUT || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Trng thAi nAy khA'ng th` hy");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            Room room = booking.getRoom();
            room.setIsOccupied(false);
            roomRepository.save(room);
        }

        return bookingRepository.save(booking);
    }
}
