package com.hotelchain.pro.property.controller;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.response.ApiResponse;
import com.hotelchain.pro.common.response.PagedResponse;
import com.hotelchain.pro.property.dto.*;
import com.hotelchain.pro.property.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Module 2 — Property Management API.
 * Quản lý chi nhánh, phòng, sơ đồ tầng.
 */
@Tag(name = "Properties", description = "Quản lý cơ sở / chi nhánh")
@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    /** GET /api/v1/properties — Danh sách chi nhánh */
    @Operation(summary = "Danh sách chi nhánh")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER')")
    public ResponseEntity<ApiResponse<List<PropertyDto>>> listProperties(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.listProperties(user)));
    }

    /** POST /api/v1/properties — Tạo chi nhánh mới */
    @Operation(summary = "Tạo chi nhánh mới")
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER')")
    public ResponseEntity<ApiResponse<PropertyDto>> createProperty(
            @Valid @RequestBody CreatePropertyRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.created(propertyService.createProperty(request, user)));
    }

    /** GET /api/v1/properties/{id} — Chi tiết chi nhánh */
    @Operation(summary = "Chi tiết chi nhánh")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'ACCOUNTANT', 'HOUSEKEEPING', 'MAINTENANCE')")
    public ResponseEntity<ApiResponse<PropertyDto>> getProperty(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.getProperty(id)));
    }

    /** PUT /api/v1/properties/{id} — Cập nhật chi nhánh */
    @Operation(summary = "Cập nhật chi nhánh")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<PropertyDto>> updateProperty(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePropertyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", propertyService.updateProperty(id, request)));
    }

    /** DELETE /api/v1/properties/{id} — Vô hiệu hóa chi nhánh */
    @Operation(summary = "Vô hiệu hóa chi nhánh")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deactivateProperty(@PathVariable UUID id) {
        propertyService.deactivateProperty(id);
        return ResponseEntity.ok(ApiResponse.noContent("Chi nhánh đã được vô hiệu hóa"));
    }

    /** POST /api/v1/properties/{id}/images — Upload ảnh chi nhánh */
    @Operation(summary = "Upload ảnh chi nhánh")
    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.success("Upload ảnh thành công",
                propertyService.uploadImages(id, files)));
    }

    /** GET /api/v1/properties/{id}/dashboard — Dashboard chi nhánh */
    @Operation(summary = "Dashboard tổng quan chi nhánh")
    @GetMapping("/{id}/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> getPropertyDashboard(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.getPropertyDashboard(id)));
    }

    /** GET /api/v1/properties/{id}/occupancy — Tỷ lệ lấp đầy */
    @Operation(summary = "Tỷ lệ lấp đầy chi nhánh")
    @GetMapping("/{id}/occupancy")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    public ResponseEntity<ApiResponse<Object>> getOccupancy(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.getOccupancy(id)));
    }
}
