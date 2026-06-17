package com.hotelchain.pro.notification.service;

import com.hotelchain.pro.common.enums.NotificationType;
import com.hotelchain.pro.notification.entity.Notification;
import com.hotelchain.pro.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Notification Dispatcher — gửi thông báo đa kênh async.
 *
 * Mapping sự kiện → kênh:
 * BOOKING_CONFIRMED    → Email + SMS + Push
 * PAYMENT_RECEIVED     → Email + Push + Zalo OA
 * CHECKOUT_REMINDER    → Push + SMS (2 tiếng trước)
 * NEW_BOOKING_ALERT    → Push (cho lễ tân)
 * PAYMENT_OVERDUE      → SMS + Email
 * ROOM_NEEDS_CLEANING  → Push (cho housekeeping)
 * MAINTENANCE_ALERT    → Push (cho kỹ thuật)
 * SHIFT_REMINDER       → Push + SMS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final EmailService emailService;
    private final SmsService smsService;
    private final FcmPushService fcmPushService;
    private final NotificationRepository notificationRepository;

    /**
     * Dispatch thông báo theo event type.
     */
    @Async
    public void dispatch(NotificationEvent event) {
        log.info("Dispatching notification type={}, userId={}", event.getType(), event.getUserId());

        Notification notification = new Notification();
        notification.setUserId(event.getUserId());
        notification.setType(event.getType());
        notification.setTitle(event.getTitle());
        notification.setBody(event.getBody());
        notification.setEntityType(event.getEntityType());
        notification.setEntityId(event.getEntityId());

        List<NotificationChannel> channels = getChannelsForType(event.getType());

        // Email
        if (channels.contains(NotificationChannel.EMAIL) && event.getRecipientEmail() != null) {
            try {
                emailService.sendHtmlEmail(event.getRecipientEmail(), event.getTitle(), event.getBody());
                notification.setSentViaEmail(true);
            } catch (Exception e) {
                log.error("Email notification failed: {}", e.getMessage());
            }
        }

        // SMS qua Esms.vn / Twilio / Viettel SMS
        if (channels.contains(NotificationChannel.SMS) && event.getRecipientPhone() != null) {
            try {
                smsService.send(event.getRecipientPhone(), event.getBody());
                notification.setSentViaSms(true);
            } catch (Exception e) {
                log.error("SMS notification failed: {}", e.getMessage());
            }
        }

        // Push Notification qua Firebase FCM
        if (channels.contains(NotificationChannel.PUSH) && event.getDeviceTokens() != null) {
            try {
                fcmPushService.sendPush(event.getDeviceTokens(), event.getTitle(), event.getBody(), event.getData());
                notification.setSentViaPush(true);
            } catch (Exception e) {
                log.error("Push notification failed: {}", e.getMessage());
            }
        }

        // Zalo OA (phù hợp thị trường Việt Nam)
        if (channels.contains(NotificationChannel.ZALO) && event.getRecipientPhone() != null) {
            try {
                // zaloService.sendZNS(event.getRecipientPhone(), templateId, data);
                notification.setSentViaZalo(true);
            } catch (Exception e) {
                log.error("Zalo OA notification failed: {}", e.getMessage());
            }
        }

        notificationRepository.save(notification);
    }

    /**
     * Xác định kênh gửi theo loại thông báo.
     */
    private List<NotificationChannel> getChannelsForType(NotificationType type) {
        return switch (type) {
            case BOOKING_CONFIRMED -> List.of(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.PUSH);
            case PAYMENT_RECEIVED -> List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH, NotificationChannel.ZALO);
            case CHECKOUT_REMINDER -> List.of(NotificationChannel.PUSH, NotificationChannel.SMS);
            case NEW_BOOKING_ALERT -> List.of(NotificationChannel.PUSH);
            case PAYMENT_OVERDUE -> List.of(NotificationChannel.SMS, NotificationChannel.EMAIL);
            case ROOM_NEEDS_CLEANING -> List.of(NotificationChannel.PUSH);
            case MAINTENANCE_ALERT -> List.of(NotificationChannel.PUSH);
            case SHIFT_REMINDER -> List.of(NotificationChannel.PUSH, NotificationChannel.SMS);
            default -> List.of(NotificationChannel.PUSH);
        };
    }

    public enum NotificationChannel {
        EMAIL, SMS, PUSH, ZALO
    }
}
