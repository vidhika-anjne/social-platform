package com.vidhika.chat_service.model;

import com.vidhika.chat_service.enums.ConversationType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    private String id;

    private ConversationType type; // hackathon, one to one, group

    private String name;

    @Indexed(sparse = true)
    private String hackathonId;

    @Indexed(sparse = true)
    private String teamId;

    private String createdBy;

    @CreatedDate
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @LastModifiedDate
    private LocalDateTime updatedAt;
}