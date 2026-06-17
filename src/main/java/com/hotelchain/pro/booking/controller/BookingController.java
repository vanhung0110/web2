package com.hotelchain.pro.booking.controller;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.booking.dto.*;
import com.hotelchain.pro.booking.service.BookingService;
import com.hotelchain.pro.common.response.ApiResponse;
import com.hotelchain.pro.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Module 3 — Booking & Check-in/Out API.
 * Quản lý đặt phòng, check-in, check-out, gia hạn, đổi phòng.
 */
@Tag(name = "Bookings", description = "Quản lý đặt phòng")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    private static final String RECEPTIONIST_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')";

    /** GET /api/v1/bookings — Danh sách đặt phòng */
    @Operation(summary = "Danh sách đặt phòng với filter/search")
    @GetMapping
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<PagedResponse<BookingDto>> listBookings(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var filter = BookingFilter.builder()
                .propertyId(propertyId)
                .status(status)
                .searchQuery(q)
                .from(from)
                .to(to)
                .build();
        return ResponseEntity.ok(new PagedResponse<>(bookingService.listBookings(filter, pageable, user)));
    }

    /** POST /api/v1/bookings — Tạo đặt phòng */
    @Operation(summary = "Tạo đặt phòng mới")
    @PostMapping
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<BookingDto>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.created(bookingService.createBooking(request, user)));
    }

    /** GET /api/v1/bookings/{id} — Chi tiết đặt phòng */
    @Operation(summary = "Chi tiết đặt phòng")
    @GetMapping("/{id}")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<BookingDto>> getBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBooking(id)));
    }

    /** PUT /api/v1/bookings/{id}/confirm — Xác nhận đặt phòng */
    @Operation(summary = "Xác nhận đặt phòng")
    @PutMapping("/{id}/confirm")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<BookingDto>> confirmBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thành công", bookingService.confirmBooking(id)));
    }

    /** PUT /api/v1/bookings/{id}/check-in — Check-in */
    @Operation(summary = "Check-in khách")
    @PutMapping("/{id}/check-in")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<BookingDto>> checkIn(
            @PathVariable UUID id,
            @Valid @RequestBody CheckInRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Check-in thành công", bookingService.checkIn(id, request, user)));
    }

    /** PUT /api/v1/bookings/{id}/check-out — Check-out */
    @Operation(summary = "Check-out khách và tạo hóa đơn")
    @PutMapping("/{id}/check-out")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<CheckOutResponse>> checkOut(
            @PathVariable UUID id,
            @Valid @RequestBody CheckOutRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Check-out thành công", bookingService.checkOut(id, request, user)));
    }

    /** PUT /api/v1/bookings/{id}/cancel — Hủy đặt phòng */
    @Operation(summary = "Hủy đặt phòng")
    @PutMapping("/{id}/cancel")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<BookingDto>> cancelBooking(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Hủy đặt phòng thành công",
                bookingService.cancelBooking(id, request != null ? request.getReason() : "")));
    }

    /** PUT /api/v1/bookings/{id}/extend — Gia hạn lưu trú */
    @Operation(summary = "Gia hạn lưu trú")
    @PutMapping("/{id}/extend")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<BookingDto>> extendBooking(
            @PathVariable UUID id,
            @Valid @RequestBody ExtendBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Gia hạn thành công", bookingService.extendBooking(id, request)));
    }

    /** PUT /api/v1/bookings/{id}/change-room — Đổi phòng */
    @Operation(summary = "Đổi phòng")
    @PutMapping("/{id}/change-room")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<BookingDto>> changeRoom(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoomRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đổi phòng thành công", bookingService.changeRoom(id, request)));
    }

    /** GET /api/v1/bookings/{id}/invoice — Xem hóa đơn */
    @Operation(summary = "Xem hóa đơn")
    @GetMapping("/{id}/invoice")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<Object>> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getInvoice(id)));
    }

    /** GET /api/v1/bookings/{id}/invoice/pdf — Tải hóa đơn PDF */
    @Operation(summary = "Tải hóa đơn PDF")
    @GetMapping("/{id}/invoice/pdf")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable UUID id) {
        byte[] pdfBytes = bookingService.generateInvoicePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
