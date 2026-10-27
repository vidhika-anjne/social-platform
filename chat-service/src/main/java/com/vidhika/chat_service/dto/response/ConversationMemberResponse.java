package com.vidhika.chat_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMemberResponse {

    private String id;
    private String conversationId;
    private String userId;
    private LocalDateTime joinedAt;
}
