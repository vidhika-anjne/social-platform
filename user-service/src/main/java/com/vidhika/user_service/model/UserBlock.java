package com.vidhika.user_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "user_blocks",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_blocker_blocked",
        columnNames = {"blocker_id", "blocked_id"}
    ),
    indexes = {
        @Index(name = "idx_blocker_id", columnList = "blocker_id"),
        @Index(name = "idx_blocked_id", columnList = "blocked_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "blocker_id", nullable = false)
    private UUID blockerId; // The user who initiated the block

    @Column(name = "blocked_id", nullable = false)
    private UUID blockedId; // The user who was blocked

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
