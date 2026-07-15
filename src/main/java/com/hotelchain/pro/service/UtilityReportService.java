package com.hotelchain.pro.service;

import com.hotelchain.pro.dto.SubmitReportRequest;
import com.hotelchain.pro.entity.*;
import com.hotelchain.pro.enums.ReportStatus;
import com.hotelchain.pro.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UtilityReportService {

    private final UtilityReportRepository reportRepository;
    private final TenantRepository tenantRepository;
    private final UtilityPriceRepository priceRepository;
    private final UserRepository userRepository;

    /**
     * User gửi báo cáo điện nước hàng tháng.
     */
    @Transactional
    public UtilityReport submitReport(UUID userId, SubmitReportRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Tenant tenant = tenantRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new RuntimeException("Bạn chưa được gán phòng. Liên hệ admin."));

        // Kiểm tra báo cáo đã tồn tại chưa (bỏ qua nếu đã bị TỪ CHỐI)
        reportRepository.findByRoomIdAndReportMonthAndReportYearAndStatusNot(
                tenant.getRoom().getId(), request.getReportMonth(), request.getReportYear(), ReportStatus.REJECTED
        ).ifPresent(r -> {
            throw new RuntimeException("Báo cáo tháng " + request.getReportMonth() + "/" + request.getReportYear() + " đã tồn tại và đang chờ duyệt/hoặc đã duyệt");
        });

        // Validate chỉ số
        if (request.getWaterNew() < request.getWaterOld()) {
            throw new RuntimeException("Chỉ số nước mới phải lớn hơn chỉ số cũ");
        }
        if (request.getElectricNew() < request.getElectricOld()) {
            throw new RuntimeException("Chỉ số điện mới phải lớn hơn chỉ số cũ");
        }

        // Lấy đơn giá
        UtilityPrice price = priceRepository.findFirstByOrderByEffectiveFromDesc()
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình đơn giá điện nước"));

        double waterUsage = request.getWaterNew() - request.getWaterOld();
        double electricUsage = request.getElectricNew() - request.getElectricOld();
        BigDecimal waterCost = price.getWaterPricePerUnit().multiply(BigDecimal.valueOf(waterUsage));
        BigDecimal electricCost = price.getElectricPricePerUnit().multiply(BigDecimal.valueOf(electricUsage));
        BigDecimal roomRent = tenant.getRoom().getMonthlyRent();
        BigDecimal internetFee = price.getInternetFee() != null ? price.getInternetFee() : BigDecimal.ZERO;
        BigDecimal trashFee = price.getTrashFee() != null ? price.getTrashFee() : BigDecimal.ZERO;
        BigDecimal totalCost = waterCost.add(electricCost).add(roomRent).add(internetFee).add(trashFee);

        UtilityReport report = new UtilityReport();
        report.setTenant(tenant);
        report.setRoom(tenant.getRoom());
        report.setReportMonth(request.getReportMonth());
        report.setReportYear(request.getReportYear());
        report.setWaterOld(request.getWaterOld());
        report.setWaterNew(request.getWaterNew());
        report.setWaterPhotoKey(request.getWaterPhotoKey());
        report.setElectricOld(request.getElectricOld());
        report.setElectricNew(request.getElectricNew());
        report.setElectricPhotoKey(request.getElectricPhotoKey());
        report.setWaterUsage(waterUsage);
        report.setElectricUsage(electricUsage);
        report.setWaterCost(waterCost);
        report.setElectricCost(electricCost);
        report.setRoomRent(roomRent);
        report.setInternetFee(internetFee);
        report.setTrashFee(trashFee);
        report.setTotalCost(totalCost);
        report.setStatus(ReportStatus.PENDING);
        report.setSubmittedBy(user);
        report.setSubmittedAt(LocalDateTime.now());
        report.setNote(request.getNote());

        return reportRepository.save(report);
    }

    /**
     * Admin duyệt báo cáo.
     */
    @Transactional
    public UtilityReport approveReport(UUID reportId, UUID adminId) {
        UtilityReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Báo cáo không tồn tại"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new RuntimeException("Báo cáo đã được xử lý (trạng thái: " + report.getStatus() + ")");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin không tồn tại"));

        report.setStatus(ReportStatus.APPROVED);
        report.setReviewedBy(admin);
        report.setReviewedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    /**
     * Admin từ chối báo cáo.
     */
    @Transactional
    public UtilityReport rejectReport(UUID reportId, UUID adminId, String reason) {
        UtilityReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Báo cáo không tồn tại"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new RuntimeException("Báo cáo đã được xử lý (trạng thái: " + report.getStatus() + ")");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin không tồn tại"));

        report.setStatus(ReportStatus.REJECTED);
        report.setRejectReason(reason);
        report.setReviewedBy(admin);
        report.setReviewedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    /** Lấy tất cả báo cáo (admin) */
    public List<UtilityReport> getAllReports() {
        return reportRepository.findAllOrderByDateDesc();
    }

    /** Lấy báo cáo pending (admin) */
    public List<UtilityReport> getPendingReports() {
        return reportRepository.findByStatusOrderBySubmittedAtAsc(ReportStatus.PENDING);
    }

    /** Lấy lịch sử báo cáo đã thanh toán (admin) */
    public List<UtilityReport> getPaidReports() {
        return reportRepository.findAllOrderByDateDesc().stream()
                .filter(r -> r.getIsPaid() != null && r.getIsPaid())
                .toList();
    }

    /** Lấy doanh thu theo tháng (admin) */
    public List<Map<String, Object>> getMonthlyRevenue() {
        return reportRepository.getMonthlyRevenue();
    }

    /** Lấy lịch sử báo cáo của user */
    public List<UtilityReport> getReportsByUser(UUID userId) {
        return reportRepository.findByUserId(userId);
    }

    /** Lấy lịch sử báo cáo của phòng */
    public List<UtilityReport> getReportsByRoom(UUID roomId) {
        return reportRepository.findByRoomIdOrderByReportYearDescReportMonthDesc(roomId);
    }

    /** Đếm báo cáo pending */
    public long countPending() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    /** Admin xác nhận đã thu tiền */
    @Transactional
    public UtilityReport markAsPaid(UUID reportId, UUID adminId) {
        UtilityReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Báo cáo không tồn tại"));

        if (report.getStatus() != ReportStatus.APPROVED) {
            throw new RuntimeException("Chỉ có thể thu tiền hóa đơn đã duyệt");
        }
        if (report.getIsPaid() != null && report.getIsPaid()) {
            throw new RuntimeException("Hóa đơn này đã được thu tiền");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin không tồn tại"));

        report.setIsPaid(true);
        report.setPaymentDate(LocalDateTime.now());
        return reportRepository.save(report);
    }
}
