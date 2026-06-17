package com.hotelchain.pro.utility.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UtilityPhotoResult {
    private String objectKey;
    private String imageHash;
    private LocalDateTime capturedAt;
    private String gpsLocation;
}
