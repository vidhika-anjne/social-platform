package com.vidhika.user_service.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryContributionStats {
    private String repositoryId;
    private String repositoryName;
    private String repositoryOwner;
    private String nameWithOwner;
    private String repositoryUrl;
    private boolean verified;
    private int pullRequestsCreated;
    private int pullRequestsMerged;
}
