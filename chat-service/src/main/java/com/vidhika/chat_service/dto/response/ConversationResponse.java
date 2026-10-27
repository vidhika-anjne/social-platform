package com.vidhika.chat_service.dto.response;

import com.vidhika.chat_service.enums.ConversationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {

    private String id;
    private ConversationType type;
    private String name;
    private String hackathonId;
    private String teamId;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<ConversationMemberResponse> members;
    private MessageResponse lastMessage;
}
