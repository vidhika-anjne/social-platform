package com.vidhika.chat_service.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent {

    private String eventId;
    private String eventType; // e.g., "CHAT_MESSAGE_SENT"
    private String conversationId;
    private String conversationType; // "ONE_TO_ONE", "GROUP", etc.
    private String senderId;
    private List<String> recipientIds; // Member user IDs excluding the sender
    private String messageId;
    private String content;
    private LocalDateTime createdAt;
}
