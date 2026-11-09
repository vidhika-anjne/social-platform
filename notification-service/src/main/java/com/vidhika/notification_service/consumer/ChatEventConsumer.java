package com.vidhika.notification_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.notification_service.dto.kafka.ChatMessageEvent;
import com.vidhika.notification_service.model.Notification;
import com.vidhika.notification_service.repository.NotificationRepository;
import com.vidhika.notification_service.service.NovuNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
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

                // 1. Persist notification in database
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

                // 2. Trigger Novu workflow for real-time in-app & push notification
                novuNotificationService.trigger(
                        "chat-message-received",
                        recipientId,
                        Map.of(
                                "conversationId", event.getConversationId(),
                                "conversationType", event.getConversationType() != null ? event.getConversationType() : "DIRECT",
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
