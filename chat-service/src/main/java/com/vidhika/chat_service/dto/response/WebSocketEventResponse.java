package com.vidhika.chat_service.dto.response;

import com.vidhika.chat_service.enums.WebSocketAction;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketEventResponse {

    private WebSocketAction action;
    private String conversationId;
    private String senderId;
    private Object payload;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
