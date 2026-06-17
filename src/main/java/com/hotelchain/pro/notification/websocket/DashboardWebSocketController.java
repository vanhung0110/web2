package com.hotelchain.pro.notification.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocket Real-time Dashboard Controller.
 *
 * Client subscribe: /topic/property/{id}/dashboard
 * Server push khi có sự kiện:
 * - Booking mới
 * - Check-in/out
 * - Thanh toán nhận được
 * - Phòng cần dọn
 * - Cảnh báo bảo trì
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final com.hotelchain.pro.report.service.DashboardService dashboardService;

    /**
     * Push cập nhật dashboard mỗi 30 giây cho tất cả admin đang online.
     */
    @Scheduled(fixedRate = 30000)
    public void broadcastDashboardUpdate() {
        try {
            // Lấy danh sách properties active để push
            dashboardService.getActivePropertyIds().forEach(propertyId -> {
                try {
                    Object dashboardData = dashboardService.getPropertyDashboard(propertyId);
                    messagingTemplate.convertAndSend(
                            "/topic/property/" + propertyId + "/dashboard",
                            dashboardData
                    );
                } catch (Exception e) {
                    log.debug("Failed to push dashboard for property {}: {}", propertyId, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Dashboard broadcast error: {}", e.getMessage());
        }
    }

    /**
     * Push real-time event khi có booking mới.
     */
    public void pushNewBooking(UUID propertyId, Object bookingData) {
        messagingTemplate.convertAndSend(
                "/topic/property/" + propertyId + "/events",
                Map.of("type", "NEW_BOOKING", "data", bookingData)
        );
    }

    /**
     * Push real-time event khi check-in.
     */
    public void pushCheckIn(UUID propertyId, Object bookingData) {
        messagingTemplate.convertAndSend(
                "/topic/property/" + propertyId + "/events",
                Map.of("type", "CHECK_IN", "data", bookingData)
        );
    }

    /**
     * Push real-time event khi thanh toán.
     */
    public void pushPaymentReceived(UUID propertyId, Object paymentData) {
        messagingTemplate.convertAndSend(
                "/topic/property/" + propertyId + "/events",
                Map.of("type", "PAYMENT_RECEIVED", "data", paymentData)
        );
    }

    /**
     * Push real-time notification đến user cụ thể.
     */
    public void pushUserNotification(UUID userId, Object notificationData) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notificationData
        );
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        log.debug("New WebSocket subscription: {}", headerAccessor.getDestination());
    }
}
