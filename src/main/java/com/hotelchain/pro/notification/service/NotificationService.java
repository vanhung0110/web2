package com.hotelchain.pro.notification.service;

import com.hotelchain.pro.notification.entity.Notification;
import com.hotelchain.pro.notification.repository.NotificationRepository;
import com.hotelchain.pro.common.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Transactional
    public void sendManual(Object request) {
        if (request instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) request;
            UUID userId = UUID.fromString(map.get("userId").toString());
            String title = map.get("title").toString();
            String body = map.get("body").toString();
            Object typeObj = map.get("type");
            String typeStr = typeObj != null ? typeObj.toString() : "SYSTEM_ALERT";
            NotificationType type = NotificationType.valueOf(typeStr);

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setBody(body);
            notification.setType(type);
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);

            // Gửi qua dispatcher
            try {
                NotificationEvent event = NotificationEvent.builder()
                        .type(type)
                        .recipientEmail(map.containsKey("email") ? map.get("email").toString() : null)
                        .recipientPhone(map.containsKey("phone") ? map.get("phone").toString() : null)
                        .title(title)
                        .body(body)
                        .build();
                notificationDispatcher.dispatch(event);
            } catch (Exception e) {
                // log error
            }
        }
    }

    public Page<Object> listNotifications(UUID userId, Pageable pageable) {
        // Trả về Page<Object> như controller mong đợi
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(n -> (Object) n);
    }

    @Transactional
    public void markAsRead(UUID id, UUID userId) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setIsRead(true);
                n.setReadAt(LocalDateTime.now());
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void deleteNotification(UUID id, UUID userId) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                notificationRepository.delete(n);
            }
        });
    }
}
