package com.hotelchain.pro.report.service;

import com.hotelchain.pro.common.exception.ResourceNotFoundException;
import com.hotelchain.pro.property.entity.Property;
import com.hotelchain.pro.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final PropertyRepository propertyRepository;

    public Object getRevenueReport(UUID propertyId, String from, String to, String groupBy) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId.toString()));

        return Map.of(
                "propertyId", propertyId,
                "period", Map.of("from", from, "to", to),
                "groupBy", groupBy,
                "totalRevenue", BigDecimal.ZERO,
                "roomRevenue", BigDecimal.ZERO,
                "utilityRevenue", BigDecimal.ZERO,
                "serviceRevenue", BigDecimal.ZERO,
                "dailyBreakdown", List.of()
        );
    }

    public Object getOccupancyReport(UUID propertyId, String from, String to) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId.toString()));

        return Map.of(
                "propertyId", propertyId,
                "period", Map.of("from", from, "to", to),
                "averageOccupancyRate", 0.0,
                "totalRooms", 0,
                "occupiedRoomDays", 0L,
                "availableRoomDays", 0L
        );
    }

    public Object getUtilityReport(UUID propertyId, String from, String to) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId.toString()));

        return Map.of(
                "propertyId", propertyId,
                "period", Map.of("from", from, "to", to),
                "waterUsageTotal", 0.0,
                "waterCostTotal", BigDecimal.ZERO,
                "electricUsageTotal", 0.0,
                "electricCostTotal", BigDecimal.ZERO
        );
    }

    public Object getBookingsReport(UUID propertyId, String from, String to) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId.toString()));

        return Map.of(
                "propertyId", propertyId,
                "period", Map.of("from", from, "to", to),
                "totalBookings", 0L,
                "statusBreakdown", Map.of(
                        "PENDING", 0L,
                        "CONFIRMED", 0L,
                        "CHECKED_IN", 0L,
                        "CHECKED_OUT", 0L,
                        "CANCELLED", 0L
                )
        );
    }

    public byte[] exportExcel(UUID propertyId, String from, String to, String type) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Báo cáo " + type);
            sheet.createRow(0).createCell(0).setCellValue("Báo cáo " + type.toUpperCase());
            sheet.createRow(1).createCell(0).setCellValue("Chi nhánh ID: " + propertyId.toString());
            sheet.createRow(2).createCell(0).setCellValue("Thời gian: từ " + from + " đến " + to);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("Excel export error: {}", e.getMessage());
            return new byte[0];
        }
    }

    public byte[] exportPdf(UUID propertyId, String from, String to, String type) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(bos);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

            document.add(new com.itextpdf.layout.element.Paragraph("BÁO CÁO HỆ THỐNG - " + type.toUpperCase()));
            document.add(new com.itextpdf.layout.element.Paragraph("Chi nhánh ID: " + propertyId.toString()));
            document.add(new com.itextpdf.layout.element.Paragraph("Thời gian: từ " + from + " đến " + to));
            document.add(new com.itextpdf.layout.element.Paragraph("Ngày xuất bản: " + LocalDateTime.now().toString()));

            document.close();
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("PDF export error: {}", e.getMessage());
            return new byte[0];
        }
    }
}
