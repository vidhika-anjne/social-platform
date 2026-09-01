package com.vidhika.user_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "follows",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_follower_following",
        columnNames = {"follower_id", "following_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "follower_id", nullable = false)
    private UUID followerId;   // The user who is following

    @Column(name = "following_id", nullable = false)
    private UUID followingId;  // The user being followed

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
