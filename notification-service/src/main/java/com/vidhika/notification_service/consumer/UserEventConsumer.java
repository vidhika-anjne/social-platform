package com.vidhika.notification_service.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.notification_service.dto.kafka.UserFollowedEvent;
import com.vidhika.notification_service.model.Notification;
import com.vidhika.notification_service.model.NotificationPreference;
import com.vidhika.notification_service.repository.NotificationPreferenceRepository;
import com.vidhika.notification_service.repository.NotificationRepository;
import com.vidhika.notification_service.service.NovuNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Kafka consumer for the user-events topic.
 * Listens for user-related domain events like USER_FOLLOWED and USER_REGISTERED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NovuNotificationService novuNotificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topic.user-events:user-events}", groupId = "notification-user-group")
    public void consumeUserEvent(String message) {
        log.info("Received Kafka event from user-events topic: {}", message);

        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();

            if ("USER_FOLLOWED".equals(eventType)) {
                UserFollowedEvent event = objectMapper.treeToValue(root, UserFollowedEvent.class);
                handleUserFollowed(event);
            } else if ("USER_REGISTERED".equals(eventType)) {
                handleUserRegistered(root);
            } else {
                log.info("Unhandled eventType from user-events: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message from user-events: {}", message, e);
        }
    }

    private void handleUserFollowed(UserFollowedEvent event) {
        if (event.getTargetUserId() == null || event.getFollowerId() == null) {
            log.warn("Invalid USER_FOLLOWED event, missing follower or target user ID: {}", event);
            return;
        }

        // Avoid notifying self
        if (event.getTargetUserId().equals(event.getFollowerId())) {
            return;
        }

        String followerDisplayName = (event.getFollowerName() != null && !event.getFollowerName().isBlank())
                ? event.getFollowerName()
                : "Someone";

        // 1. Persist notification in database for in-app notification center
        Notification notification = Notification.builder()
                .recipientId(event.getTargetUserId())
                .actorId(event.getFollowerId())
                .type("USER_FOLLOWED")
                .message(followerDisplayName + " started following you.")
                .read(false)
                .build();

        notificationRepository.save(notification);

        // 2. Check recipient preferences & DND filter
        Optional<NotificationPreference> optPref = preferenceRepository.findByUserId(event.getTargetUserId());
        boolean pushEnabled = optPref.map(NotificationPreference::isPushEnabled).orElse(true);
        boolean isDndActive = optPref.map(p -> p.isDndActiveNow(LocalTime.now())).orElse(false);

        if (!pushEnabled) {
            log.info("Push notifications disabled for recipient {}. Skipping Novu follow dispatch.",
                    event.getTargetUserId());
            return;
        }

        if (isDndActive) {
            log.info("DND active for recipient {}. Skipping Novu follow dispatch.", event.getTargetUserId());
            return;
        }

        // 3. Dispatch to Novu
        Map<String, Object> payload = new HashMap<>();
        payload.put("followerId", event.getFollowerId().toString());
        payload.put("followerName", followerDisplayName);
        payload.put("followerAvatar", event.getFollowerAvatar() != null ? event.getFollowerAvatar() : "");
        payload.put("message", followerDisplayName + " started following you.");

        novuNotificationService.trigger(
                "user-followed",
                event.getTargetUserId(),
                payload
        );

        log.info("Dispatched USER_FOLLOWED notification to Novu for target user {} (from follower {})",
                event.getTargetUserId(), event.getFollowerId());
    }

    private void handleUserRegistered(JsonNode root) {
        String userIdStr = root.path("userId").asText(null);
        if (userIdStr == null || userIdStr.isBlank()) {
            userIdStr = root.path("aggregateId").asText(null);
        }

        if (userIdStr != null && !userIdStr.isBlank()) {
            String email = root.path("email").asText(null);
            String name = root.path("name").asText(null);

            novuNotificationService.identifySubscriber(userIdStr, name, null, email, null);
            log.info("Synced newly registered subscriber [{}] ({}) to Novu", userIdStr, email);
        }
    }
}

