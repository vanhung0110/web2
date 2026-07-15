package com.hotelchain.pro.controller;

import com.hotelchain.pro.dto.ApiResponse;
import com.hotelchain.pro.dto.CreateBookingRequest;
import com.hotelchain.pro.entity.Booking;
import com.hotelchain.pro.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings() {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getAllBookings()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("T\u1EA1o \u0111\u1EB7t c\u1ECDc th\u00E0nh c\u00F4ng", bookingService.createBooking(request)));
    }

    @PutMapping("/{id}/{action}")
    public ResponseEntity<ApiResponse<?>> processAction(@PathVariable UUID id, @PathVariable String action) {
        Booking booking;
        switch (action) {
            case "checkin":
                booking = bookingService.checkIn(id);
                return ResponseEntity.ok(ApiResponse.ok("Nh\u1EADn ph\u00F2ng v\u00E0 chuy\u1EC3n th\u00E0nh kh\u00E1ch thu\u00EA th\u00E0nh c\u00F4ng", booking));
            case "checkout":
                booking = bookingService.checkOut(id);
                return ResponseEntity.ok(ApiResponse.ok("Tr\u1EA3 ph\u00F2ng th\u00E0nh c\u00F4ng", booking));
            case "cancel":
                booking = bookingService.cancelBooking(id);
                return ResponseEntity.ok(ApiResponse.ok("H\u1EE7y \u0111\u1EB7t c\u1ECDc th\u00E0nh c\u00F4ng", booking));
            default:
                return ResponseEntity.badRequest().body(ApiResponse.error("Hành động không hợp lệ"));
        }
    }
}
