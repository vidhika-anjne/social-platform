package com.vidhika.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubRepoContributionDetailResponse {
    private String repositoryId;
    private String nameWithOwner;
    private String repositoryUrl;
    private boolean verified;
    private int pullRequestsCreated;
    private int pullRequestsMerged;
}
