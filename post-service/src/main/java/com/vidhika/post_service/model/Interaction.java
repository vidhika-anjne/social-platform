package com.vidhika.post_service.model;

import com.vidhika.post_service.enums.InteractionType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "user_post_interactions")
@Entity
public class Interaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private UUID postId;
    @Column(nullable = false,name = "interaction_type")
    @Enumerated(EnumType.STRING)
    private InteractionType type;
    @Column(name = "watch_duration_ms")
    private Long watchDurationMs;
    @Column(nullable = false,name = "created_at")
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
