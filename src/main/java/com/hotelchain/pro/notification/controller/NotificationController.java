package com.hotelchain.pro.notification.controller;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.response.ApiResponse;
import com.hotelchain.pro.common.response.PagedResponse;
import com.hotelchain.pro.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Module 8 — Notification API.
 */
@Tag(name = "Notifications", description = "Thông báo đa kênh")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** POST /api/v1/notifications/send — Gửi thông báo thủ công */
    @Operation(summary = "Gửi thông báo thủ công")
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendNotification(@RequestBody Object request) {
        notificationService.sendManual(request);
        return ResponseEntity.ok(ApiResponse.noContent("Thông báo đã được gửi"));
    }

    /** GET /api/v1/notifications — Danh sách thông báo */
    @Operation(summary = "Danh sách thông báo của người dùng hiện tại")
    @GetMapping
    public ResponseEntity<PagedResponse<Object>> listNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(new PagedResponse<>(notificationService.listNotifications(user.getId(), pageable)));
    }

    /** PUT /api/v1/notifications/{id}/read — Đánh dấu đã đọc */
    @Operation(summary = "Đánh dấu thông báo đã đọc")
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.noContent("Đã đánh dấu đã đọc"));
    }

    /** PUT /api/v1/notifications/read-all — Đánh dấu tất cả đã đọc */
    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc")
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.noContent("Đã đánh dấu tất cả đã đọc"));
    }

    /** DELETE /api/v1/notifications/{id} — Xóa thông báo */
    @Operation(summary = "Xóa thông báo")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.ok(ApiResponse.noContent("Xóa thông báo thành công"));
    }
}
