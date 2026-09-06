package com.vidhika.notificaiton_service.service;

import com.vidhika.notificaiton_service.dto.NotificationResponse;
import com.vidhika.notificaiton_service.dto.UnreadCountResponse;
import com.vidhika.notificaiton_service.exception.NotificationNotFoundException;
import com.vidhika.notificaiton_service.model.Notification;
import com.vidhika.notificaiton_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> getNotifications(UUID recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UnreadCountResponse getUnreadCount(UUID recipientId) {
        long count = notificationRepository.countByRecipientIdAndReadFalse(recipientId);
        return UnreadCountResponse.builder()
                .unreadCount(count)
                .build();
    }

    @Transactional
    public NotificationResponse markAsRead(UUID id, UUID recipientId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new NotificationNotFoundException(id.toString()));

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }

    @Transactional
    public void markAllAsRead(UUID recipientId) {
        notificationRepository.markAllAsReadForRecipient(recipientId);
    }

    @Transactional
    public void deleteNotification(UUID id, UUID recipientId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new NotificationNotFoundException(id.toString()));

        notificationRepository.delete(notification);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipientId())
                .type(notification.getType())
                .message(notification.getMessage())
                .postId(notification.getPostId())
                .actorId(notification.getActorId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
