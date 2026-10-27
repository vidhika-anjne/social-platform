package com.vidhika.chat_service.dto.response;

import com.vidhika.chat_service.enums.MessageStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private String id;
    private String conversationId;
    private String senderId;
    private String content;
    private LocalDateTime createdAt;
    private MessageStatus status;
}
