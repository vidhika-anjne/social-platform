package com.vidhika.user_service.github.scheduler;

import com.vidhika.user_service.github.GitHubClient;
import com.vidhika.user_service.github.dto.RepositoryContributionStats;
import com.vidhika.user_service.model.GithubContribution;
import com.vidhika.user_service.model.UserProfile;
import com.vidhika.user_service.repository.GithubContributionRepository;
import com.vidhika.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Periodically synchronizes selected GitHub repository contributions in bounded batches.
 * Local database functions as a read model; GitHub remains the source of truth.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GithubContributionSyncScheduler {

    private final GithubContributionRepository githubContributionRepository;
    private final UserProfileRepository userProfileRepository;
    private final GitHubClient gitHubClient;

    @Value("${github.sync.batch-size:100}")
    private int batchSize = 100;

    @Value("${github.sync.base-backoff-minutes:5}")
    private long baseBackoffMinutes = 5;

    @Value("${github.sync.max-backoff-minutes:360}")
    private long maxBackoffMinutes = 360;

    @Value("${github.sync.success-interval-hours:24}")
    private long successIntervalHours = 24;

    /**
     * Executes periodically with a fixed delay between the completion of the previous run
     * and the start of the next run (default: 5 minutes = 300,000ms).
     * Only fetches a bounded batch of records where selected = true and nextSyncAt <= now.
     */
    @Scheduled(
            fixedDelayString = "${github.sync.fixed-delay-ms:300000}",
            initialDelayString = "${github.sync.initial-delay-ms:10000}"
    )
    public void syncDueContributions() {
        LocalDateTime now = LocalDateTime.now();
        List<GithubContribution> dueContributions = githubContributionRepository.findDueContributions(
                now, PageRequest.of(0, batchSize));

        if (dueContributions.isEmpty()) {
            log.debug("No GitHub contributions due for sync at {}", now);
            return;
        }

        log.info("Found {} GitHub contribution(s) due for sync. Processing bounded batch...", dueContributions.size());

        // Process sequentially to respect GitHub API rate limits and avoid socket exhaustion
        for (GithubContribution contribution : dueContributions) {
            syncSingleContribution(contribution, now);
        }

        log.info("Completed syncing batch of {} contribution(s).", dueContributions.size());
    }

    /**
     * Synchronizes a single contribution record with GitHub.
     */
    public void syncSingleContribution(GithubContribution contribution, LocalDateTime now) {
        String repoIdentifier = contribution.getRepositoryOwner() + "/" + contribution.getRepositoryName();

        try {
            Optional<UserProfile> profileOpt = userProfileRepository.findById(contribution.getUserId());
            if (profileOpt.isEmpty()) {
                handleFailure(contribution, "User profile not found for userId: " + contribution.getUserId(), now);
                return;
            }

            UserProfile profile = profileOpt.get();
            if (profile.getGithubUsername() == null || profile.getGithubUsername().isBlank()) {
                handleFailure(contribution, "No connected GitHub username found on profile for user: " + contribution.getUserId(), now);
                return;
            }

            // Fetch latest PR statistics from GitHub (GraphQL)
            RepositoryContributionStats stats = gitHubClient.getRepositoryContributionStats(
                    profile.getGithubAccessToken(),
                    profile.getGithubUsername(),
                    contribution.getRepositoryOwner(),
                    contribution.getRepositoryName()
            );

            // On success: update counts, reset retry counters, schedule next sync in 24 hours
            contribution.setPullRequestsCreated(stats.getPullRequestsCreated());
            contribution.setPullRequestsMerged(stats.getPullRequestsMerged());
            contribution.setLastSyncedAt(now);
            contribution.setNextSyncAt(now.plusHours(successIntervalHours));
            contribution.setSyncAttempts(0);
            contribution.setLastSyncStatus("SUCCESS");
            contribution.setLastSyncError(null);

            githubContributionRepository.save(contribution);
            log.info("Successfully synced GitHub contribution for repo {} (User {}): {} created, {} merged",
                    repoIdentifier, contribution.getUserId(), stats.getPullRequestsCreated(), stats.getPullRequestsMerged());

        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("Failed to sync GitHub contribution for repo {} (User {}): {}",
                    repoIdentifier, contribution.getUserId(), errorMsg);
            handleFailure(contribution, errorMsg, now);
        }
    }

    /**
     * Handles sync failure with exponential backoff while preserving existing statistics.
     */
    private void handleFailure(GithubContribution contribution, String errorMessage, LocalDateTime now) {
        int attempts = contribution.getSyncAttempts() + 1;
        contribution.setSyncAttempts(attempts);

        long delayMinutes = calculateBackoffMinutes(attempts);
        contribution.setNextSyncAt(now.plusMinutes(delayMinutes));
        contribution.setLastSyncStatus("FAILED");
        contribution.setLastSyncError(truncate(errorMessage, 1000));

        githubContributionRepository.save(contribution);
        log.info("Scheduled retry #{} for repo {}/{} at {} (backoff {} mins)",
                attempts, contribution.getRepositoryOwner(), contribution.getRepositoryName(),
                contribution.getNextSyncAt(), delayMinutes);
    }

    /**
     * Calculates exponential backoff delay in minutes:
     * base * 2^(attempts - 1), capped at maxBackoffMinutes.
     */
    public long calculateBackoffMinutes(int attempts) {
        if (attempts <= 0) attempts = 1;
        int exponent = Math.min(attempts - 1, 10);
        long multiplier = 1L << exponent;
        long delay = baseBackoffMinutes * multiplier;
        return Math.min(delay, maxBackoffMinutes);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}