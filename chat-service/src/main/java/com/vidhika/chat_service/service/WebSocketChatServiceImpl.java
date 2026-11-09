package com.vidhika.chat_service.service;

import com.vidhika.chat_service.dto.kafka.ChatMessageEvent;
import com.vidhika.chat_service.dto.request.MessageStatusUpdateRequest;
import com.vidhika.chat_service.dto.request.SendMessageRequest;
import com.vidhika.chat_service.dto.request.TypingRequest;
import com.vidhika.chat_service.dto.response.MessageResponse;
import com.vidhika.chat_service.dto.response.WebSocketEventResponse;
import com.vidhika.chat_service.enums.MessageStatus;
import com.vidhika.chat_service.enums.WebSocketAction;
import com.vidhika.chat_service.model.ConversationMember;
import com.vidhika.chat_service.producer.ChatEventProducer;
import com.vidhika.chat_service.repository.ConversationMemberRepository;
import com.vidhika.chat_service.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketChatServiceImpl implements WebSocketChatService {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatEventProducer chatEventProducer;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final PresenceService presenceService;

    @Override
    public MessageResponse processAndBroadcastMessage(String senderId, String conversationId, SendMessageRequest request) {
        log.info("Processing WebSocket message for conversation {} from user {}", conversationId, senderId);

        // 1. Persist the message first (enforcing membership & storage in MongoDB)
        MessageResponse persistedMessage = messageService.saveMessage(senderId, conversationId, request.getContent());

        // 2. Broadcast to /topic/chat/{conversationId}
        String destination = "/topic/chat/" + conversationId;
        messagingTemplate.convertAndSend(destination, persistedMessage);
        log.info("Broadcasted message {} to destination {}", persistedMessage.getId(), destination);

        // 3. Emit CHAT_MESSAGE_SENT event to Kafka for notifications
        try {
            List<String> recipientIds = conversationMemberRepository.findByConversationId(conversationId).stream()
                    .filter(member -> !member.getUserId().equals(senderId))
                    // 1. Suppress if member has muted this conversation
                    .filter(member -> !member.isMuted())
                    // 2. Suppress if member is actively viewing this exact chat room right now
                    .filter(member -> !presenceService.isUserInRoom(member.getUserId(), conversationId))
                    .map(ConversationMember::getUserId)
                    .collect(Collectors.toList());

            if (!recipientIds.isEmpty()) {
                String conversationType = conversationRepository.findById(conversationId)
                        .map(c -> c.getType() != null ? c.getType().name() : "DIRECT")
                        .orElse("DIRECT");

                ChatMessageEvent event = ChatMessageEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("CHAT_MESSAGE_SENT")
                        .conversationId(conversationId)
                        .conversationType(conversationType)
                        .senderId(senderId)
                        .recipientIds(recipientIds)
                        .messageId(persistedMessage.getId())
                        .content(persistedMessage.getContent())
                        .createdAt(persistedMessage.getCreatedAt())
                        .build();

                chatEventProducer.publishMessageSentEvent(event);
            } else {
                log.debug("All recipients for conversation {} are currently active in the room or muted. Notification event skipped.", conversationId);
            }
        } catch (Exception e) {
            log.error("Failed to prepare/dispatch chat event for message {}: {}", persistedMessage.getId(), e.getMessage(), e);
        }

        return persistedMessage;
    }

    @Override
    public void broadcastTyping(String senderId, String conversationId, TypingRequest request) {
        String destination = "/topic/chat/" + conversationId + "/typing";

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", WebSocketAction.TYPING.name());
        payload.put("conversationId", conversationId);
        payload.put("userId", senderId);
        payload.put("typing", request.isTyping());
        payload.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSend(destination, payload);
        log.debug("Broadcasted typing event for user {} in conversation {}", senderId, conversationId);
    }

    @Override
    public void broadcastDelivered(String currentUserId, String conversationId, MessageStatusUpdateRequest request) {
        String destination = "/topic/chat/" + conversationId + "/delivered";

        if (request != null && request.getMessageId() != null && !request.getMessageId().isBlank()) {
            messageService.updateMessageStatus(currentUserId, conversationId, request.getMessageId(), MessageStatus.DELIVERED);
        } else {
            messageService.markMessagesAsDelivered(currentUserId, conversationId);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", WebSocketAction.DELIVERED.name());
        payload.put("conversationId", conversationId);
        payload.put("userId", currentUserId);
        payload.put("messageId", request != null ? request.getMessageId() : null);
        payload.put("status", MessageStatus.DELIVERED.name());
        payload.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSend(destination, payload);
        log.debug("Broadcasted delivered status for conversation {}", conversationId);
    }

    @Override
    public void broadcastRead(String currentUserId, String conversationId, MessageStatusUpdateRequest request) {
        String destination = "/topic/chat/" + conversationId + "/read";

        if (request != null && request.getMessageId() != null && !request.getMessageId().isBlank()) {
            messageService.updateMessageStatus(currentUserId, conversationId, request.getMessageId(), MessageStatus.READ);
        } else {
            messageService.markMessagesAsRead(currentUserId, conversationId);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", WebSocketAction.READ.name());
        payload.put("conversationId", conversationId);
        payload.put("userId", currentUserId);
        payload.put("messageId", request != null ? request.getMessageId() : null);
        payload.put("status", MessageStatus.READ.name());
        payload.put("timestamp", LocalDateTime.now());

        messagingTemplate.convertAndSend(destination, payload);
        log.debug("Broadcasted read status for conversation {}", conversationId);
    }
}
