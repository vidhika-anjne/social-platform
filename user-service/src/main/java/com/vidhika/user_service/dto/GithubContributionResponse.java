package com.vidhika.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubContributionResponse {

    private UUID id;
    private UUID userId;
    private String githubRepositoryId;
    private String repositoryName;
    private String repositoryOwner;
    private String repositoryUrl;
    private int pullRequestsCreated;
    private int pullRequestsMerged;
    private boolean selected;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime nextSyncAt;
    private int syncAttempts;
    private String lastSyncStatus;
    private String lastSyncError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
