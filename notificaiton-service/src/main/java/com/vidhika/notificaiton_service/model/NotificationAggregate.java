package com.vidhika.notificaiton_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "notification_aggregates",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_aggregate_window",
        columnNames = {"recipient_id", "post_id", "notification_type", "window_start"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "last_actor_id")
    private UUID lastActorId;

    @Column(name = "like_count", nullable = false)
    private int count;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Column(nullable = false)
    private boolean notified;
}
