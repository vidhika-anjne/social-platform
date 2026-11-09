package com.vidhika.notification_service.dto.kafka;

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
    private String eventType;
    private String conversationId;
    private String conversationType;
    private String senderId;
    private List<String> recipientIds;
    private String messageId;
    private String content;
    private LocalDateTime createdAt;
}
