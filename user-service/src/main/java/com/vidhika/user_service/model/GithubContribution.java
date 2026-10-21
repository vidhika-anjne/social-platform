package com.vidhika.user_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "github_contributions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_github_repo", columnNames = {"user_id", "github_repository_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GithubContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "github_repository_id", nullable = false)
    private String githubRepositoryId;

    @Column(name = "repository_name", nullable = false)
    private String repositoryName;

    @Column(name = "repository_owner", nullable = false)
    private String repositoryOwner;

    @Column(name = "repository_url", nullable = false)
    private String repositoryUrl;

    @Column(name = "pull_requests_created", nullable = false)
    private int pullRequestsCreated;

    @Column(name = "pull_requests_merged", nullable = false)
    private int pullRequestsMerged;

    @Column(nullable = false)
    @Builder.Default
    private boolean selected = true;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "next_sync_at")
    private LocalDateTime nextSyncAt;

    @Column(name = "sync_attempts", nullable = false)
    @Builder.Default
    private int syncAttempts = 0;

    @Column(name = "last_sync_status")
    private String lastSyncStatus;

    @Column(name = "last_sync_error", length = 1000)
    private String lastSyncError;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
