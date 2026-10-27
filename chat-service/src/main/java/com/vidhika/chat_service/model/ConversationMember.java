package com.vidhika.chat_service.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "conversation_members")
@CompoundIndex(name = "conv_user_unique_idx", def = "{'conversationId': 1, 'userId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMember {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    @Indexed
    private String userId;

    @CreatedDate
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
}
