package com.hotelchain.pro.utility.controller;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.response.ApiResponse;
import com.hotelchain.pro.utility.dto.*;
import com.hotelchain.pro.utility.service.UtilityPhotoService;
import com.hotelchain.pro.utility.service.UtilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Module 5 — Utility & Meter Reading API.
 */
@Tag(name = "Utility", description = "Quản lý đồng hồ nước/điện")
@RestController
@RequestMapping("/api/v1/utility")
@RequiredArgsConstructor
public class UtilityController {

    private final UtilityPhotoService photoService;
    private final UtilityService utilityService;

    private static final String RECEPTIONIST_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')";

    /** POST /api/v1/utility/upload-photo — Upload ảnh đồng hồ */
    @Operation(summary = "Upload ảnh đồng hồ nước/điện")
    @PostMapping(value = "/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<UtilityPhotoResult>> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,      // WATER / ELECTRIC
            @RequestParam("bookingId") UUID bookingId,
            @RequestParam("period") String period)  // START / END
    {
        UtilityPhotoService.UtilityType utilityType = UtilityPhotoService.UtilityType.valueOf(type.toUpperCase());
        UtilityPhotoService.ReadingPeriod readingPeriod = UtilityPhotoService.ReadingPeriod.valueOf(period.toUpperCase());
        var result = photoService.uploadAndValidate(file, utilityType, bookingId, readingPeriod);
        return ResponseEntity.ok(ApiResponse.success("Upload ảnh thành công", result));
    }

    /** POST /api/v1/utility/readings/start — Ghi chỉ số đầu kỳ */
    @Operation(summary = "Ghi chỉ số đầu kỳ (check-in)")
    @PostMapping("/readings/start")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<Object>> recordStartReading(
            @Valid @RequestBody StartReadingRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Ghi chỉ số đầu kỳ thành công",
                utilityService.recordStartReading(request, user)));
    }

    /** POST /api/v1/utility/readings/end — Ghi chỉ số cuối kỳ */
    @Operation(summary = "Ghi chỉ số cuối kỳ (check-out)")
    @PostMapping("/readings/end")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<Object>> recordEndReading(
            @Valid @RequestBody EndReadingRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Ghi chỉ số cuối kỳ thành công",
                utilityService.recordEndReading(request, user)));
    }

    /** GET /api/v1/utility/readings/{bookingId} — Xem chỉ số booking */
    @Operation(summary = "Xem chỉ số đồng hồ của booking")
    @GetMapping("/readings/{bookingId}")
    @PreAuthorize(RECEPTIONIST_ROLES)
    public ResponseEntity<ApiResponse<Object>> getReadings(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.success(utilityService.getReadings(bookingId)));
    }

    /** PUT /api/v1/utility/readings/{id}/verify — Xác nhận chỉ số */
    @Operation(summary = "Admin xác nhận chỉ số đồng hồ")
    @PutMapping("/readings/{id}/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> verifyReading(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thành công",
                utilityService.verifyReading(id)));
    }

    /** PUT /api/v1/utility/readings/{id}/dispute — Ghi nhận tranh chấp */
    @Operation(summary = "Ghi nhận tranh chấp chỉ số đồng hồ")
    @PutMapping("/readings/{id}/dispute")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> disputeReading(
            @PathVariable UUID id,
            @RequestBody DisputeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận tranh chấp thành công",
                utilityService.disputeReading(id, request)));
    }

    /** GET /api/v1/utility/rooms/{roomId}/history — Lịch sử đồng hồ theo phòng */
    @Operation(summary = "Lịch sử đồng hồ theo phòng")
    @GetMapping("/rooms/{roomId}/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<Object>>> getRoomHistory(@PathVariable UUID roomId) {
        return ResponseEntity.ok(ApiResponse.success(utilityService.getRoomHistory(roomId)));
    }

    /** GET /api/v1/utility/prices — Giá điện nước hiện tại */
    @Operation(summary = "Xem giá điện nước hiện tại")
    @GetMapping("/prices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> getPrices(
            @RequestParam UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.success(utilityService.getPrices(propertyId)));
    }

    /** PUT /api/v1/utility/prices — Cập nhật giá điện nước */
    @Operation(summary = "Cập nhật giá điện nước")
    @PutMapping("/prices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> updatePrices(
            @RequestParam UUID propertyId,
            @Valid @RequestBody UpdatePricesRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giá thành công",
                utilityService.updatePrices(propertyId, request)));
    }
}
