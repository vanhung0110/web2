package com.hotelchain.pro.report.controller;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.response.ApiResponse;
import com.hotelchain.pro.report.service.DashboardService;
import com.hotelchain.pro.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Module 7 — Report & Analytics API.
 */
@Tag(name = "Reports", description = "Báo cáo & phân tích")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final DashboardService dashboardService;
    private final ReportService reportService;

    /** GET /api/v1/dashboard/chain — Dashboard toàn chuỗi */
    @GetMapping("/api/v1/dashboard/chain")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER')")
    @Operation(summary = "Dashboard tổng quan toàn chuỗi")
    public ResponseEntity<ApiResponse<Object>> getChainDashboard(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getChainDashboard(user)));
    }

    /** GET /api/v1/dashboard/property/{id} — Dashboard chi nhánh */
    @GetMapping("/api/v1/dashboard/property/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Dashboard chi tiết chi nhánh")
    public ResponseEntity<ApiResponse<Object>> getPropertyDashboard(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getPropertyDashboard(id)));
    }

    /** GET /api/v1/reports/revenue — Báo cáo doanh thu */
    @GetMapping("/api/v1/reports/revenue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Báo cáo doanh thu theo kỳ")
    public ResponseEntity<ApiResponse<Object>> getRevenueReport(
            @RequestParam UUID propertyId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "DAILY") String groupBy) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getRevenueReport(propertyId, from, to, groupBy)));
    }

    /** GET /api/v1/reports/occupancy — Báo cáo công suất */
    @GetMapping("/api/v1/reports/occupancy")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Báo cáo công suất phòng (Occupancy Rate)")
    public ResponseEntity<ApiResponse<Object>> getOccupancyReport(
            @RequestParam UUID propertyId,
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getOccupancyReport(propertyId, from, to)));
    }

    /** GET /api/v1/reports/utility — Báo cáo điện nước */
    @GetMapping("/api/v1/reports/utility")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Báo cáo tiêu thụ điện nước")
    public ResponseEntity<ApiResponse<Object>> getUtilityReport(
            @RequestParam UUID propertyId,
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getUtilityReport(propertyId, from, to)));
    }

    /** GET /api/v1/reports/bookings — Báo cáo đặt phòng */
    @GetMapping("/api/v1/reports/bookings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER')")
    @Operation(summary = "Báo cáo đặt phòng")
    public ResponseEntity<ApiResponse<Object>> getBookingsReport(
            @RequestParam UUID propertyId,
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getBookingsReport(propertyId, from, to)));
    }

    /** GET /api/v1/reports/export/excel — Xuất Excel */
    @GetMapping("/api/v1/reports/export/excel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Xuất báo cáo dạng Excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam UUID propertyId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "revenue") String type) {
        byte[] excelBytes = reportService.exportExcel(propertyId, from, to, type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + type + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    /** GET /api/v1/reports/export/pdf — Xuất PDF */
    @GetMapping("/api/v1/reports/export/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_OWNER', 'CHAIN_MANAGER', 'PROPERTY_MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Xuất báo cáo dạng PDF")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam UUID propertyId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "revenue") String type) {
        byte[] pdfBytes = reportService.exportPdf(propertyId, from, to, type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + type + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
