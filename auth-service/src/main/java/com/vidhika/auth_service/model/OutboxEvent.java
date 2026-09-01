package com.vidhika.auth_service.model;

import com.vidhika.auth_service.enums.OutboxStatus;
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
}