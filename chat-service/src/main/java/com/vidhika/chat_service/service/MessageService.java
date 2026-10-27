package com.vidhika.chat_service.service;

import com.vidhika.chat_service.dto.response.MessageResponse;
import com.vidhika.chat_service.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageService {

    Page<MessageResponse> getMessageHistory(String currentUserId, String conversationId, Pageable pageable);

    MessageResponse saveMessage(String senderId, String conversationId, String content);

    MessageResponse updateMessageStatus(String currentUserId, String conversationId, String messageId, MessageStatus status);

    void markMessagesAsDelivered(String currentUserId, String conversationId);

    void markMessagesAsRead(String currentUserId, String conversationId);
}