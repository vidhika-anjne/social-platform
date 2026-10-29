package com.vidhika.chat_service.service;

import com.vidhika.chat_service.dto.request.MessageStatusUpdateRequest;
import com.vidhika.chat_service.dto.request.SendMessageRequest;
import com.vidhika.chat_service.dto.request.TypingRequest;
import com.vidhika.chat_service.dto.response.MessageResponse;

public interface WebSocketChatService {

    MessageResponse processAndBroadcastMessage(String senderId, String conversationId, SendMessageRequest request);

    void broadcastTyping(String senderId, String conversationId, TypingRequest request);

    void broadcastDelivered(String currentUserId, String conversationId, MessageStatusUpdateRequest request);

    void broadcastRead(String currentUserId, String conversationId, MessageStatusUpdateRequest request);
}