package com.vidhika.post_service.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vidhika.post_service.dto.OutboxEventRequest;
import com.vidhika.post_service.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "outbox_events")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    private UUID id;

    private UUID aggregateId;

    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private LocalDateTime createdAt;

    private boolean processed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;
    public static OutboxEvent outboxEventBuilder(OutboxEventRequest outboxEventRequest) throws JsonProcessingException
    {
        return OutboxEvent.builder()
                .id(outboxEventRequest.getEventId())
                .aggregateId(outboxEventRequest.getAggregateId())
                .eventType("POST_CLEANUP")
                .payload(outboxEventRequest.getPayload())
                .createdAt(outboxEventRequest.getCreatedAt())
                .processed(false)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }
}