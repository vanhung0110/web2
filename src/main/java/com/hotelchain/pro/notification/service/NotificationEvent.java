package com.hotelchain.pro.notification.service;

import com.hotelchain.pro.common.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class NotificationEvent {
    private UUID userId;
    private NotificationType type;
    private String title;
    private String body;
    private String recipientEmail;
    private String recipientPhone;
    private List<String> deviceTokens;
    private Map<String, String> data;
    private String entityType;
    private UUID entityId;
}
