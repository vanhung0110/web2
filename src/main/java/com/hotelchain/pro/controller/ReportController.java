package com.hotelchain.pro.controller;

import com.hotelchain.pro.dto.ApiResponse;
import com.hotelchain.pro.dto.ReviewReportRequest;
import com.hotelchain.pro.dto.SubmitReportRequest;
import com.hotelchain.pro.entity.UtilityReport;
import com.hotelchain.pro.service.UtilityReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final UtilityReportService reportService;

    /** User gửi báo cáo điện nước */
    @PostMapping
    public ResponseEntity<ApiResponse<UtilityReport>> submitReport(
            @Valid @RequestBody SubmitReportRequest request) {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok("Gửi báo cáo thành công, đợi admin duyệt",
                reportService.submitReport(userId, request)));
    }

    /** Lấy lịch sử báo cáo của user */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<UtilityReport>>> getMyReports() {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getReportsByUser(userId)));
    }

    /** Admin: lấy tất cả báo cáo */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UtilityReport>>> getAllReports() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getAllReports()));
    }

    /** Admin: lấy lịch sử thanh toán */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<UtilityReport>>> getPaidReports() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getPaidReports()));
    }

    /** Admin: lấy báo cáo chờ duyệt */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<UtilityReport>>> getPendingReports() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getPendingReports()));
    }

    /** Admin: duyệt báo cáo */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<UtilityReport>> approveReport(@PathVariable UUID id) {
        UUID adminId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok("Đã duyệt báo cáo",
                reportService.approveReport(id, adminId)));
    }

    /** Admin: từ chối báo cáo */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<UtilityReport>> rejectReport(
            @PathVariable UUID id,
            @RequestBody ReviewReportRequest request) {
        UUID adminId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối báo cáo",
                reportService.rejectReport(id, adminId, request.getRejectReason())));
    }

    /** Admin: xác nhận đã thu tiền */
    @PutMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<UtilityReport>> markAsPaid(@PathVariable UUID id) {
        UUID adminId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok("Đã xác nhận thu tiền",
                reportService.markAsPaid(id, adminId)));
    }

    /** Lấy báo cáo theo phòng */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<UtilityReport>>> getReportsByRoom(@PathVariable UUID roomId) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getReportsByRoom(roomId)));
    }

    /** Đếm pending */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("pending", reportService.countPending())));
    }

    /** Admin: Lấy doanh thu theo tháng */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMonthlyRevenue() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getMonthlyRevenue()));
    }
}
