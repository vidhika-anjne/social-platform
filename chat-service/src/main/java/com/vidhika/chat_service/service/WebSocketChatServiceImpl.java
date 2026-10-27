package com.vidhika.chat_service.service;

import com.vidhika.chat_service.dto.request.MessageStatusUpdateRequest;
import com.vidhika.chat_service.dto.request.SendMessageRequest;
import com.vidhika.chat_service.dto.request.TypingRequest;
import com.vidhika.chat_service.dto.response.MessageResponse;
import com.vidhika.chat_service.dto.response.WebSocketEventResponse;
import com.vidhika.chat_service.enums.MessageStatus;
import com.vidhika.chat_service.enums.WebSocketAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketChatServiceImpl implements WebSocketChatService {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public MessageResponse processAndBroadcastMessage(String senderId, String conversationId, SendMessageRequest request) {
        log.info("Processing WebSocket message for conversation {} from user {}", conversationId, senderId);

        // 1. Persist the message first (enforcing membership & storage in MongoDB)
        MessageResponse persistedMessage = messageService.saveMessage(senderId, conversationId, request.getContent());

        // 2. Broadcast to /topic/chat/{conversationId}
        String destination = "/topic/chat/" + conversationId;
        messagingTemplate.convertAndSend(destination, persistedMessage);
        log.info("Broadcasted message {} to destination {}", persistedMessage.getId(), destination);

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
