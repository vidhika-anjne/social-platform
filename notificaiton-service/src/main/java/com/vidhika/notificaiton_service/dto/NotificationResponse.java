package com.vidhika.notificaiton_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private UUID id;
    private UUID recipientId;
    private String type;
    private String message;
    private UUID postId;
    private UUID actorId;
    private boolean read;
    private LocalDateTime createdAt;
}
