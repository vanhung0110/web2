package com.hotelchain.pro.staff.controller;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.response.ApiResponse;
import com.hotelchain.pro.common.response.PagedResponse;
import com.hotelchain.pro.staff.dto.*;
import com.hotelchain.pro.staff.service.StaffService;
import com.hotelchain.pro.staff.service.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Module 6 — Staff & Shift API.
 */
@Tag(name = "Staff", description = "Quản lý nhân sự và ca làm việc")
@RestController
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;
    private final ShiftService shiftService;

    private static final String MANAGER_ROLES =
            "hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')";

    // ===== Staff Management =====

    /** GET /api/v1/staff — Danh sách nhân viên */
    @GetMapping("/api/v1/staff")
    @PreAuthorize(MANAGER_ROLES)
    @Operation(summary = "Danh sách nhân viên")
    public ResponseEntity<ApiResponse<Object>> listStaff(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                staffService.listStaff(propertyId, PageRequest.of(page, size))));
    }

    /** POST /api/v1/staff — Thêm nhân viên */
    @PostMapping("/api/v1/staff")
    @PreAuthorize(MANAGER_ROLES)
    @Operation(summary = "Thêm nhân viên mới")
    public ResponseEntity<ApiResponse<Object>> createStaff(
            @Valid @RequestBody CreateStaffRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.created(staffService.createStaff(request, user)));
    }

    /** PUT /api/v1/staff/{id} — Cập nhật nhân viên */
    @PutMapping("/api/v1/staff/{id}")
    @PreAuthorize(MANAGER_ROLES)
    @Operation(summary = "Cập nhật thông tin nhân viên")
    public ResponseEntity<ApiResponse<Object>> updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", staffService.updateStaff(id, request)));
    }

    /** PUT /api/v1/staff/{id}/activate — Kích hoạt/vô hiệu hóa */
    @PutMapping("/api/v1/staff/{id}/activate")
    @PreAuthorize(MANAGER_ROLES)
    @Operation(summary = "Kích hoạt hoặc vô hiệu hóa nhân viên")
    public ResponseEntity<ApiResponse<Void>> toggleActivation(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        staffService.setActive(id, active);
        return ResponseEntity.ok(ApiResponse.noContent(active ? "Kích hoạt thành công" : "Vô hiệu hóa thành công"));
    }

    /** PUT /api/v1/staff/{id}/reset-password — Đặt lại mật khẩu */
    @PutMapping("/api/v1/staff/{id}/reset-password")
    @PreAuthorize(MANAGER_ROLES)
    @Operation(summary = "Đặt lại mật khẩu nhân viên")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@PathVariable UUID id) {
        String tempPassword = staffService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.success("Đã đặt lại mật khẩu tạm thời",
                java.util.Map.of("temporaryPassword", tempPassword)));
    }

    // ===== Shift Management =====

    /** GET /api/v1/shifts — Lịch ca làm việc */
    @GetMapping("/api/v1/shifts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'ACCOUNTANT', 'HOUSEKEEPING', 'MAINTENANCE')")
    @Operation(summary = "Lịch ca làm việc")
    public ResponseEntity<ApiResponse<Object>> listShifts(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.listShifts(propertyId, from, to)));
    }

    /** POST /api/v1/shifts — Tạo ca làm việc */
    @PostMapping("/api/v1/shifts")
    @PreAuthorize(MANAGER_ROLES)
    @Operation(summary = "Tạo ca làm việc")
    public ResponseEntity<ApiResponse<Object>> createShift(
            @Valid @RequestBody CreateShiftRequest request) {
        return ResponseEntity.ok(ApiResponse.created(shiftService.createShift(request)));
    }

    /** POST /api/v1/shifts/{id}/clock-in — Chấm công vào */
    @PostMapping("/api/v1/shifts/{id}/clock-in")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'ACCOUNTANT', 'HOUSEKEEPING', 'MAINTENANCE')")
    @Operation(summary = "Chấm công vào ca")
    public ResponseEntity<ApiResponse<Object>> clockIn(
            @PathVariable UUID id,
            @RequestBody(required = false) ClockInRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Chấm công vào thành công",
                shiftService.clockIn(id, request, user)));
    }

    /** POST /api/v1/shifts/{id}/clock-out — Chấm công ra */
    @PostMapping("/api/v1/shifts/{id}/clock-out")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'RECEPTIONIST', 'ACCOUNTANT', 'HOUSEKEEPING', 'MAINTENANCE')")
    @Operation(summary = "Chấm công ra ca")
    public ResponseEntity<ApiResponse<Object>> clockOut(
            @PathVariable UUID id,
            @RequestBody(required = false) ClockOutRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Chấm công ra thành công",
                shiftService.clockOut(id, request, user)));
    }

    /** GET /api/v1/attendance — Báo cáo chấm công */
    @GetMapping("/api/v1/attendance")
    @PreAuthorize(MANAGER_ROLES)
    @Operation(summary = "Báo cáo chấm công nhân viên")
    public ResponseEntity<ApiResponse<Object>> getAttendanceReport(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(ApiResponse.success(shiftService.getAttendanceReport(propertyId, from, to)));
    }
}
