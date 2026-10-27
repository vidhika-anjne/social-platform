package com.vidhika.chat_service.dto.request;

import com.vidhika.chat_service.enums.ConversationType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConversationRequest {

    @NotNull(message = "Conversation type is required")
    private ConversationType type;

    private String name;

    private String recipientId; // Used for ONE_TO_ONE

    private List<String> memberIds; // Used for GROUP

    private String hackathonId;
}
