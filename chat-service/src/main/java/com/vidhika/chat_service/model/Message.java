package com.vidhika.chat_service.model;

import com.vidhika.chat_service.enums.MessageStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "messages")
@CompoundIndex(name = "conv_created_idx", def = "{'conversationId': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    private String id;

    private String conversationId;

    private String senderId;

    private String content;

    @CreatedDate
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;
}