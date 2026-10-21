package com.vidhika.user_service.github;

import com.vidhika.user_service.dto.*;
import com.vidhika.user_service.exception.GitHubNotConnectedException;
import com.vidhika.user_service.exception.UserNotFoundException;
import com.vidhika.user_service.github.dto.ContributionRepositoriesPage;
import com.vidhika.user_service.github.dto.RepositoryContributionStats;
import com.vidhika.user_service.model.GithubContribution;
import com.vidhika.user_service.model.UserProfile;
import com.vidhika.user_service.repository.GithubContributionRepository;
import com.vidhika.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final GitHubClient gitHubClient;
    private final UserProfileRepository userProfileRepository;
    private final GithubContributionRepository githubContributionRepository;

    /**
     * Part 1: GitHub Contribution Discovery.
     * Returns lightweight list of repositories where user made contributions.
     */
    public List<GithubRepoItem> discoverContributionRepositories(UUID userId, int first, String after) {
        UserProfile profile = getConnectedUserProfile(userId);

        ContributionRepositoriesPage page = gitHubClient.getContributionRepositories(
                profile.getGithubAccessToken(),
                profile.getGithubUsername(),
                first,
                after
        );

        return page.getRepositories();
    }

    /**
     * Returns full paginated discovery page if cursor/hasNextPage information is needed.
     */
    public ContributionRepositoriesPage discoverContributionRepositoriesPage(UUID userId, int first, String after) {
        UserProfile profile = getConnectedUserProfile(userId);

        return gitHubClient.getContributionRepositories(
                profile.getGithubAccessToken(),
                profile.getGithubUsername(),
                first,
                after
        );
    }

    /**
     * Part 2 & Part 4: Detailed contribution verification & statistics for a specific repository.
     */
    public GithubRepoContributionDetailResponse getDetailedContributionStats(
            UUID userId, String owner, String repo) {

        UserProfile profile = getConnectedUserProfile(userId);

        RepositoryContributionStats stats = gitHubClient.getRepositoryContributionStats(
                profile.getGithubAccessToken(),
                profile.getGithubUsername(),
                owner,
                repo
        );

        return GithubRepoContributionDetailResponse.builder()
                .repositoryId(stats.getRepositoryId())
                .nameWithOwner(stats.getNameWithOwner())
                .repositoryUrl(stats.getRepositoryUrl())
                .verified(stats.isVerified())
                .pullRequestsCreated(stats.getPullRequestsCreated())
                .pullRequestsMerged(stats.getPullRequestsMerged())
                .build();
    }

    /**
     * Part 3: Selecting a repository to display on Pulse profile.
     * Verifies contribution -> fetches PR stats -> saves/updates github_contributions table.
     */
    @Transactional
    public GithubContributionResponse verifyAndSaveRepositoryContribution(
            UUID userId, String owner, String repo) {

        UserProfile profile = getConnectedUserProfile(userId);

        RepositoryContributionStats stats = gitHubClient.getRepositoryContributionStats(
                profile.getGithubAccessToken(),
                profile.getGithubUsername(),
                owner,
                repo
        );

        if (!stats.isVerified()) {
            throw new IllegalArgumentException(
                    "Cannot add repository to profile: connected GitHub user '" + profile.getGithubUsername()
                            + "' has no verified contributions to " + owner + "/" + repo);
        }

        GithubContribution contribution = githubContributionRepository
                .findByUserIdAndGithubRepositoryId(userId, stats.getRepositoryId())
                .orElseGet(() -> githubContributionRepository
                        .findByUserIdAndRepositoryOwnerIgnoreCaseAndRepositoryNameIgnoreCase(userId, owner, repo)
                        .orElseGet(() -> GithubContribution.builder()
                                .userId(userId)
                                .githubRepositoryId(stats.getRepositoryId())
                                .repositoryName(stats.getRepositoryName())
                                .repositoryOwner(stats.getRepositoryOwner())
                                .repositoryUrl(stats.getRepositoryUrl())
                                .build()
                        )
                );

        contribution.setPullRequestsCreated(stats.getPullRequestsCreated());
        contribution.setPullRequestsMerged(stats.getPullRequestsMerged());
        contribution.setSelected(true);
        LocalDateTime now = LocalDateTime.now();
        contribution.setLastSyncedAt(now);
        contribution.setNextSyncAt(now.plusHours(24));
        contribution.setSyncAttempts(0);
        contribution.setLastSyncStatus("SUCCESS");
        contribution.setLastSyncError(null);

        GithubContribution saved = githubContributionRepository.save(contribution);
        log.info("Saved GitHub contribution for user {} on repo {}", userId, stats.getNameWithOwner());

        return mapToResponse(saved);
    }

    /**
     * Retrieves selected contributions from the database for display on Pulse profile.
     * Fast DB query, zero GitHub API calls.
     */
    @Transactional(readOnly = true)
    public List<GithubContributionResponse> getSelectedContributions(UUID userId) {
        return githubContributionRepository.findByUserIdAndSelectedTrueOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Unselects / removes a repository from the user's displayed profile.
     */
    @Transactional
    public void unselectContribution(UUID userId, String owner, String repo) {
        githubContributionRepository
                .findByUserIdAndRepositoryOwnerIgnoreCaseAndRepositoryNameIgnoreCase(userId, owner, repo)
                .ifPresent(contribution -> {
                    contribution.setSelected(false);
                    githubContributionRepository.save(contribution);
                    log.info("Unselected GitHub contribution for user {} on repo {}/{}", userId, owner, repo);
                });
    }

    /**
     * Connects or updates a GitHub account on the user's profile.
     */
    @Transactional
    public void connectGithubAccount(UUID userId, ConnectGithubRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User profile not found for id: " + userId));

        profile.setGithubUsername(request.getGithubUsername().trim());
        if (request.getGithubAccessToken() != null && !request.getGithubAccessToken().isBlank()) {
            profile.setGithubAccessToken(request.getGithubAccessToken().trim());
        }

        userProfileRepository.save(profile);
        log.info("Connected GitHub account '{}' for user {}", profile.getGithubUsername(), userId);
    }

    private UserProfile getConnectedUserProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User profile not found for id: " + userId));

        if (profile.getGithubUsername() == null || profile.getGithubUsername().isBlank()) {
            throw new GitHubNotConnectedException(
                    "No GitHub account connected for user. Please connect your GitHub account to access contribution features.");
        }

        return profile;
    }

    private GithubContributionResponse mapToResponse(GithubContribution entity) {
        return GithubContributionResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .githubRepositoryId(entity.getGithubRepositoryId())
                .repositoryName(entity.getRepositoryName())
                .repositoryOwner(entity.getRepositoryOwner())
                .repositoryUrl(entity.getRepositoryUrl())
                .pullRequestsCreated(entity.getPullRequestsCreated())
                .pullRequestsMerged(entity.getPullRequestsMerged())
                .selected(entity.isSelected())
                .lastSyncedAt(entity.getLastSyncedAt())
                .nextSyncAt(entity.getNextSyncAt())
                .syncAttempts(entity.getSyncAttempts())
                .lastSyncStatus(entity.getLastSyncStatus())
                .lastSyncError(entity.getLastSyncError())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
