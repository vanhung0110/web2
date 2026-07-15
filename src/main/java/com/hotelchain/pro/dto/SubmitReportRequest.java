package com.hotelchain.pro.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReportRequest {
    @NotNull(message = "Tháng báo cáo không được để trống")
    private Integer reportMonth;

    @NotNull(message = "Năm báo cáo không được để trống")
    private Integer reportYear;

    @NotNull(message = "Chỉ số nước cũ không được để trống")
    private Double waterOld;

    @NotNull(message = "Chỉ số nước mới không được để trống")
    private Double waterNew;

    @NotNull(message = "Chỉ số điện cũ không được để trống")
    private Double electricOld;

    @NotNull(message = "Chỉ số điện mới không được để trống")
    private Double electricNew;

    private String waterPhotoKey;
    private String electricPhotoKey;
    private String note;
}
