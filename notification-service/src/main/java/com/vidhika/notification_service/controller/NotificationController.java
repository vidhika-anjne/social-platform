package com.vidhika.notification_service.controller;

import com.vidhika.notification_service.dto.DeviceTokenRequest;
import com.vidhika.notification_service.dto.NotificationPreferenceRequest;
import com.vidhika.notification_service.dto.NotificationPreferenceResponse;
import com.vidhika.notification_service.dto.NotificationResponse;
import com.vidhika.notification_service.dto.UnreadCountResponse;
import com.vidhika.notification_service.service.NotificationPreferenceService;
import com.vidhika.notification_service.service.NotificationService;
import com.vidhika.notification_service.service.NovuNotificationService;
import jakarta.validation.Valid;
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
    private final NotificationPreferenceService preferenceService;
    private final NovuNotificationService novuNotificationService;

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

    // POST /api/v1/notifications/device-token (Register mobile/web push token in Novu)
    @PostMapping("/device-token")
    public ResponseEntity<Void> registerDeviceToken(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeviceTokenRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        novuNotificationService.setSubscriberCredentials(userId, request.getProvider(), request.getToken());
        return ResponseEntity.ok().build();
    }

    // GET /api/v1/notifications/preferences (Get user's notification preferences)
    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(preferenceService.getPreferences(userId));
    }

    // PUT /api/v1/notifications/preferences (Update DND, push, in-app preferences)
    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody NotificationPreferenceRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(preferenceService.updatePreferences(userId, request));
    }
}
