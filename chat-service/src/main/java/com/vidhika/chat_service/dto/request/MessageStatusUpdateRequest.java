package com.vidhika.chat_service.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageStatusUpdateRequest {

    private String messageId;
}
