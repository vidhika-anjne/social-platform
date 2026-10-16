package com.vidhika.post_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
public class OutboxEventRequest
{
    private UUID eventId;
    private String eventType;
    private UUID aggregateId;
    private UUID entityId; // contains id of Post,Like,Comment
    private String payload;
    private LocalDateTime createdAt;
}
// Generic class for OutputBox Request