package com.hotelchain.pro.repository;

import com.hotelchain.pro.entity.UtilityReport;
import com.hotelchain.pro.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilityReportRepository extends JpaRepository<UtilityReport, UUID> {

    List<UtilityReport> findByTenantIdOrderByReportYearDescReportMonthDesc(UUID tenantId);

    List<UtilityReport> findByRoomIdOrderByReportYearDescReportMonthDesc(UUID roomId);

    List<UtilityReport> findByStatusOrderBySubmittedAtAsc(ReportStatus status);

    Optional<UtilityReport> findByRoomIdAndReportMonthAndReportYearAndStatusNot(UUID roomId, Integer month, Integer year, ReportStatus status);

    @Query("SELECT r FROM UtilityReport r ORDER BY r.reportYear DESC, r.reportMonth DESC")
    List<UtilityReport> findAllOrderByDateDesc();

    long countByStatus(ReportStatus status);

    @Query("SELECT r FROM UtilityReport r WHERE r.tenant.user.id = :userId ORDER BY r.reportYear DESC, r.reportMonth DESC")
    List<UtilityReport> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT r.reportMonth as month, r.reportYear as year, SUM(r.totalCost) as revenue FROM UtilityReport r WHERE r.isPaid = true GROUP BY r.reportYear, r.reportMonth ORDER BY r.reportYear DESC, r.reportMonth DESC")
    List<Map<String, Object>> getMonthlyRevenue();
}
