package com.vidhika.notification_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.notification_service.dto.kafka.ChatMessageEvent;
import com.vidhika.notification_service.model.Notification;
import com.vidhika.notification_service.model.NotificationPreference;
import com.vidhika.notification_service.repository.NotificationPreferenceRepository;
import com.vidhika.notification_service.repository.NotificationRepository;
import com.vidhika.notification_service.service.NotificationThrottlingService;
import com.vidhika.notification_service.service.NovuNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Kafka consumer for the chat-events topic.
 * Listens for new messages sent in conversations and dispatches notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventConsumer {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationThrottlingService throttlingService;
    private final NovuNotificationService novuNotificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topic.chat-events:chat-events}", groupId = "notification-chat-group")
    public void consumeChatEvent(String message) {
        log.info("Received Kafka event from chat-events topic: {}", message);

        try {
            ChatMessageEvent event = objectMapper.readValue(message, ChatMessageEvent.class);
            if ("CHAT_MESSAGE_SENT".equals(event.getEventType())) {
                handleChatMessageSent(event);
            } else {
                log.info("Unhandled eventType from chat-events: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message from chat-events: {}", message, e);
        }
    }

    private void handleChatMessageSent(ChatMessageEvent event) {
        if (event.getRecipientIds() == null || event.getRecipientIds().isEmpty()) {
            log.debug("No recipients to notify for conversation {}", event.getConversationId());
            return;
        }

        UUID senderId;
        try {
            senderId = UUID.fromString(event.getSenderId());
        } catch (IllegalArgumentException e) {
            log.error("Invalid senderId UUID: {}", event.getSenderId());
            return;
        }

        for (String recipientIdStr : event.getRecipientIds()) {
            try {
                UUID recipientId = UUID.fromString(recipientIdStr);

                // Prevent notifying self
                if (recipientId.equals(senderId)) {
                    continue;
                }

                // 1. Persist notification in database (ensures in-app history/badge is preserved)
                Notification notification = Notification.builder()
                        .recipientId(recipientId)
                        .actorId(senderId)
                        .type("CHAT_MESSAGE")
                        .message(event.getContent())
                        .conversationId(event.getConversationId())
                        .messageId(event.getMessageId())
                        .read(false)
                        .build();

                notificationRepository.save(notification);

                // 2. Evaluate User Preferences & DND filter
                Optional<NotificationPreference> optPref = preferenceRepository.findByUserId(recipientId);
                boolean pushEnabled = optPref.map(NotificationPreference::isPushEnabled).orElse(true);
                boolean isDndActive = optPref.map(p -> p.isDndActiveNow(LocalTime.now())).orElse(false);

                if (!pushEnabled) {
                    log.info("Push notifications disabled by preference for recipient {}. Skipping Novu buzz.", recipientId);
                    continue;
                }

                if (isDndActive) {
                    log.info("DND is active for recipient {}. Skipping Novu buzz.", recipientId);
                    continue;
                }

                // 3. Anti-spam / flood throttling filter (skip rapid bursts within 5s)
                if (throttlingService.shouldThrottle(recipientId.toString(), event.getConversationId())) {
                    log.info("Chat notification throttled (5s burst) for recipient {} in conversation {}",
                            recipientId, event.getConversationId());
                    continue;
                }

                // 4. Route to appropriate Novu workflow based on conversation type
                String convType = event.getConversationType() != null ? event.getConversationType() : "DIRECT";
                String workflowId = ("ONE_TO_ONE".equalsIgnoreCase(convType) || "DIRECT".equalsIgnoreCase(convType))
                        ? "chat-direct-message"
                        : "chat-group-message";

                novuNotificationService.trigger(
                        workflowId,
                        recipientId,
                        Map.of(
                                "conversationId", event.getConversationId(),
                                "conversationType", convType,
                                "messageId", event.getMessageId(),
                                "senderId", event.getSenderId(),
                                "messagePreview", event.getContent() != null ? event.getContent() : ""
                        )
                );

                log.info("Dispatched notification for recipient {} for chat message in conversation {}",
                        recipientId, event.getConversationId());

            } catch (Exception e) {
                log.error("Failed to process chat notification for recipient {}: {}", recipientIdStr, e.getMessage(), e);
            }
        }
    }
}

