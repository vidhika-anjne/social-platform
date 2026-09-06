package com.vidhika.notificaiton_service.controller;

import com.vidhika.notificaiton_service.dto.NotificationResponse;
import com.vidhika.notificaiton_service.dto.UnreadCountResponse;
import com.vidhika.notificaiton_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // GET /api/v1/notifications
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal Jwt jwt) {
        UUID recipientId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(notificationService.getNotifications(recipientId));
    }

    // GET /api/v1/notifications/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        UUID recipientId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(notificationService.getUnreadCount(recipientId));
    }

    // PATCH /api/v1/notifications/{id}/read
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID recipientId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(notificationService.markAsRead(id, recipientId));
    }

    // PATCH /api/v1/notifications/read-all
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        UUID recipientId = UUID.fromString(jwt.getSubject());
        notificationService.markAllAsRead(recipientId);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/v1/notifications/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID recipientId = UUID.fromString(jwt.getSubject());
        notificationService.deleteNotification(id, recipientId);
        return ResponseEntity.noContent().build();
    }
}
