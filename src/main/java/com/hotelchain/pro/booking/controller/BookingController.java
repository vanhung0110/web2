package com.hotelchain.pro.booking.controller;

import com.hotelchain.pro.booking.dto.CreateBookingRequest;
import com.hotelchain.pro.booking.entity.Booking;
import com.hotelchain.pro.booking.service.BookingService;
import com.hotelchain.pro.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings() {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getAllBookings()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tạo booking thành công", bookingService.createBooking(request)));
    }

    @PutMapping("/{id}/checkin")
    public ResponseEntity<ApiResponse<Booking>> checkIn(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Check-in thành công", bookingService.checkIn(id)));
    }

    @PutMapping("/{id}/checkout")
    public ResponseEntity<ApiResponse<Booking>> checkOut(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Check-out thành công", bookingService.checkOut(id)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancelBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy booking", bookingService.cancelBooking(id)));
    }
}
