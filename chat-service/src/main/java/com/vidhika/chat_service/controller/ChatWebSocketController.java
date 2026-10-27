package com.vidhika.chat_service.controller;

import com.vidhika.chat_service.dto.request.MessageStatusUpdateRequest;
import com.vidhika.chat_service.dto.request.SendMessageRequest;
import com.vidhika.chat_service.dto.request.TypingRequest;
import com.vidhika.chat_service.service.WebSocketChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final WebSocketChatService webSocketChatService;

    /**
     * Handles sending chat messages.
     * Destination: /app/chat/{conversationId}
     * Broadcasts to: /topic/chat/{conversationId}
     */
    @MessageMapping("/chat/{conversationId}")
    public void sendMessage(
            @DestinationVariable String conversationId,
            @Valid @Payload SendMessageRequest request,
            Principal principal) {

        if (principal == null || principal.getName() == null) {
            log.error("Rejecting message send: No authenticated principal");
            throw new AccessDeniedException("Authentication required to send chat messages");
        }

        // Sender ID is derived strictly from the authenticated principal
        String senderId = principal.getName();
        log.info("Received message send request for conversation {} from user {}", conversationId, senderId);

        webSocketChatService.processAndBroadcastMessage(senderId, conversationId, request);
    }

    /**
     * Handles typing status indicators.
     * Destination: /app/chat/{conversationId}/typing
     * Broadcasts to: /topic/chat/{conversationId}/typing
     */
    @MessageMapping("/chat/{conversationId}/typing")
    public void typing(
            @DestinationVariable String conversationId,
            @Payload TypingRequest request,
            Principal principal) {

        if (principal == null || principal.getName() == null) {
            return;
        }

        String senderId = principal.getName();
        webSocketChatService.broadcastTyping(senderId, conversationId, request);
    }

    /**
     * Handles delivery confirmation.
     * Destination: /app/chat/{conversationId}/delivered
     * Broadcasts to: /topic/chat/{conversationId}/delivered
     */
    @MessageMapping("/chat/{conversationId}/delivered")
    public void delivered(
            @DestinationVariable String conversationId,
            @Payload(required = false) MessageStatusUpdateRequest request,
            Principal principal) {

        if (principal == null || principal.getName() == null) {
            return;
        }

        String userId = principal.getName();
        webSocketChatService.broadcastDelivered(userId, conversationId, request);
    }

    /**
     * Handles read receipts.
     * Destination: /app/chat/{conversationId}/read
     * Broadcasts to: /topic/chat/{conversationId}/read
     */
    @MessageMapping("/chat/{conversationId}/read")
    public void read(
            @DestinationVariable String conversationId,
            @Payload(required = false) MessageStatusUpdateRequest request,
            Principal principal) {

        if (principal == null || principal.getName() == null) {
            return;
        }

        String userId = principal.getName();
        webSocketChatService.broadcastRead(userId, conversationId, request);
    }
}
