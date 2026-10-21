package com.vidhika.user_service.github.scheduler;

import com.vidhika.user_service.exception.GitHubApiException;
import com.vidhika.user_service.github.GitHubClient;
import com.vidhika.user_service.github.dto.RepositoryContributionStats;
import com.vidhika.user_service.model.GithubContribution;
import com.vidhika.user_service.model.UserProfile;
import com.vidhika.user_service.repository.GithubContributionRepository;
import com.vidhika.user_service.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GithubContributionSyncSchedulerTest {

    private InMemoryGithubContributionRepository contributionRepository;
    private InMemoryUserProfileRepository profileRepository;
    private StubGitHubClient gitHubClient;
    private GithubContributionSyncScheduler scheduler;

    private UUID userId;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userProfile = UserProfile.builder()
                .id(userId)
                .name("Alice")
                .email("alice@pulse.com")
                .githubUsername("alice-dev")
                .githubAccessToken("gho_alice_token")
                .build();

        contributionRepository = new InMemoryGithubContributionRepository();
        profileRepository = new InMemoryUserProfileRepository();
        profileRepository.save(userProfile);

        gitHubClient = new StubGitHubClient();
        scheduler = new GithubContributionSyncScheduler(contributionRepository, profileRepository, gitHubClient);
    }

    @Test
    void syncDueContributions_emptyBatch_doesNothing() {
        scheduler.syncDueContributions();
        assertEquals(0, gitHubClient.callCount);
    }

    @Test
    void syncDueContributions_successfulSync_updatesPRCountsAndSchedulesNextSyncIn24Hours() {
        LocalDateTime now = LocalDateTime.now();

        GithubContribution contribution = GithubContribution.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .githubRepositoryId("R_123")
                .repositoryName("react")
                .repositoryOwner("facebook")
                .repositoryUrl("https://github.com/facebook/react")
                .pullRequestsCreated(5)
                .pullRequestsMerged(3)
                .selected(true)
                .nextSyncAt(now.minusMinutes(10)) // Due for sync
                .syncAttempts(1)
                .lastSyncStatus("FAILED")
                .lastSyncError("Temporary error")
                .build();

        contributionRepository.save(contribution);

        // GitHub client returns updated stats
        gitHubClient.nextStats = RepositoryContributionStats.builder()
                .repositoryId("R_123")
                .nameWithOwner("facebook/react")
                .pullRequestsCreated(8)
                .pullRequestsMerged(6)
                .verified(true)
                .build();

        scheduler.syncDueContributions();

        assertEquals(1, gitHubClient.callCount);
        GithubContribution updated = contributionRepository.findById(contribution.getId()).orElseThrow();

        // Verify updated counts
        assertEquals(8, updated.getPullRequestsCreated());
        assertEquals(6, updated.getPullRequestsMerged());

        // Verify success fields and reset retry
        assertEquals("SUCCESS", updated.getLastSyncStatus());
        assertNull(updated.getLastSyncError());
        assertEquals(0, updated.getSyncAttempts());
        assertNotNull(updated.getLastSyncedAt());

        // Verify scheduled 24 hours in the future
        assertTrue(updated.getNextSyncAt().isAfter(now.plusHours(23)));
        assertTrue(updated.getNextSyncAt().isBefore(now.plusHours(25)));
    }

    @Test
    void syncDueContributions_failure_preservesStatsAndAppliesExponentialBackoff() {
        LocalDateTime now = LocalDateTime.now();

        GithubContribution contribution = GithubContribution.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .githubRepositoryId("R_123")
                .repositoryName("react")
                .repositoryOwner("facebook")
                .repositoryUrl("https://github.com/facebook/react")
                .pullRequestsCreated(5)
                .pullRequestsMerged(3)
                .selected(true)
                .nextSyncAt(now.minusMinutes(10))
                .syncAttempts(0)
                .build();

        contributionRepository.save(contribution);

        gitHubClient.shouldThrow = new GitHubApiException("GitHub API 403 rate limit");

        scheduler.syncDueContributions();

        assertEquals(1, gitHubClient.callCount);
        GithubContribution updated = contributionRepository.findById(contribution.getId()).orElseThrow();

        // Existing stats preserved
        assertEquals(5, updated.getPullRequestsCreated());
        assertEquals(3, updated.getPullRequestsMerged());

        // Failure tracked
        assertEquals("FAILED", updated.getLastSyncStatus());
        assertTrue(updated.getLastSyncError().contains("GitHub API 403 rate limit"));
        assertEquals(1, updated.getSyncAttempts());

        // 1st retry delay = 5 minutes
        assertTrue(updated.getNextSyncAt().isAfter(now.plusMinutes(4)));
        assertTrue(updated.getNextSyncAt().isBefore(now.plusMinutes(6)));
    }

    @Test
    void syncDueContributions_missingGitHubUsername_handlesFailureWithBackoff() {
        LocalDateTime now = LocalDateTime.now();

        userProfile.setGithubUsername(null);
        profileRepository.save(userProfile);

        GithubContribution contribution = GithubContribution.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .githubRepositoryId("R_123")
                .repositoryName("react")
                .repositoryOwner("facebook")
                .repositoryUrl("https://github.com/facebook/react")
                .pullRequestsCreated(5)
                .pullRequestsMerged(3)
                .selected(true)
                .nextSyncAt(now.minusMinutes(1))
                .syncAttempts(1)
                .build();

        contributionRepository.save(contribution);

        scheduler.syncDueContributions();

        // Should not call GitHub
        assertEquals(0, gitHubClient.callCount);

        GithubContribution updated = contributionRepository.findById(contribution.getId()).orElseThrow();
        assertEquals(2, updated.getSyncAttempts());
        assertEquals("FAILED", updated.getLastSyncStatus());
        assertTrue(updated.getLastSyncError().contains("No connected GitHub username"));

        // 2nd retry delay: base (5) * 2^1 = 10 minutes
        assertTrue(updated.getNextSyncAt().isAfter(now.plusMinutes(9)));
        assertTrue(updated.getNextSyncAt().isBefore(now.plusMinutes(11)));
    }

    @Test
    void calculateBackoffMinutes_exponentialCurveAndCap() {
        assertEquals(5, scheduler.calculateBackoffMinutes(1));   // 5 * 2^0 = 5
        assertEquals(10, scheduler.calculateBackoffMinutes(2));  // 5 * 2^1 = 10
        assertEquals(20, scheduler.calculateBackoffMinutes(3));  // 5 * 2^2 = 20
        assertEquals(40, scheduler.calculateBackoffMinutes(4));  // 5 * 2^3 = 40
        assertEquals(80, scheduler.calculateBackoffMinutes(5));  // 5 * 2^4 = 80
        assertEquals(160, scheduler.calculateBackoffMinutes(6)); // 5 * 2^5 = 160
        assertEquals(320, scheduler.calculateBackoffMinutes(7)); // 5 * 2^6 = 320
        assertEquals(360, scheduler.calculateBackoffMinutes(8)); // Capped at max (360)
        assertEquals(360, scheduler.calculateBackoffMinutes(15)); // Capped at max (360)
    }

    // --- Test Doubles ---

    private static class StubGitHubClient extends GitHubClient {
        int callCount = 0;
        RepositoryContributionStats nextStats;
        RuntimeException shouldThrow;

        StubGitHubClient() {
            super("http://localhost", "", null);
        }

        @Override
        public RepositoryContributionStats getRepositoryContributionStats(String token, String username, String owner, String repo) {
            callCount++;
            if (shouldThrow != null) {
                throw shouldThrow;
            }
            return nextStats;
        }
    }

    private static class InMemoryUserProfileRepository implements UserProfileRepository {
        private final Map<UUID, UserProfile> storage = new HashMap<>();

        @Override public Optional<UserProfile> findById(UUID uuid) { return Optional.ofNullable(storage.get(uuid)); }
        @Override public <S extends UserProfile> S save(S entity) { storage.put(entity.getId(), entity); return entity; }
        @Override public Optional<UserProfile> findByEmail(String email) { return Optional.empty(); }
        @Override public boolean existsByEmail(String email) { return false; }
        @Override public boolean existsById(UUID uuid) { return storage.containsKey(uuid); }
        @Override public List<UserProfile> findAll() { return new ArrayList<>(storage.values()); }
        @Override public List<UserProfile> findAllById(Iterable<UUID> uuids) { return List.of(); }
        @Override public long count() { return storage.size(); }
        @Override public void deleteById(UUID uuid) { storage.remove(uuid); }
        @Override public void delete(UserProfile entity) { storage.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends UUID> uuids) {}
        @Override public void deleteAll(Iterable<? extends UserProfile> entities) {}
        @Override public void deleteAll() { storage.clear(); }
        @Override public void flush() {}
        @Override public <S extends UserProfile> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends UserProfile> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<UserProfile> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
        @Override public void deleteAllInBatch() {}
        @Override public UserProfile getOne(UUID uuid) { return storage.get(uuid); }
        @Override public UserProfile getById(UUID uuid) { return storage.get(uuid); }
        @Override public UserProfile getReferenceById(UUID uuid) { return storage.get(uuid); }
        @Override public <S extends UserProfile> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends UserProfile> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override public <S extends UserProfile> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends UserProfile> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends UserProfile> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends UserProfile> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends UserProfile, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends UserProfile> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public List<UserProfile> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public org.springframework.data.domain.Page<UserProfile> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }

    private static class InMemoryGithubContributionRepository implements GithubContributionRepository {
        private final Map<UUID, GithubContribution> storage = new HashMap<>();

        @Override
        public Optional<GithubContribution> findByUserIdAndGithubRepositoryId(UUID userId, String githubRepositoryId) {
            return storage.values().stream()
                    .filter(c -> c.getUserId().equals(userId) && c.getGithubRepositoryId().equals(githubRepositoryId))
                    .findFirst();
        }

        @Override
        public Optional<GithubContribution> findByUserIdAndRepositoryOwnerIgnoreCaseAndRepositoryNameIgnoreCase(UUID userId, String repositoryOwner, String repositoryName) {
            return storage.values().stream()
                    .filter(c -> c.getUserId().equals(userId)
                            && c.getRepositoryOwner().equalsIgnoreCase(repositoryOwner)
                            && c.getRepositoryName().equalsIgnoreCase(repositoryName))
                    .findFirst();
        }

        @Override
        public List<GithubContribution> findByUserIdAndSelectedTrueOrderByCreatedAtDesc(UUID userId) {
            return storage.values().stream()
                    .filter(c -> c.getUserId().equals(userId) && c.isSelected())
                    .toList();
        }

        @Override
        public List<GithubContribution> findByUserId(UUID userId) {
            return storage.values().stream()
                    .filter(c -> c.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public void deleteByUserIdAndRepositoryOwnerIgnoreCaseAndRepositoryNameIgnoreCase(UUID userId, String repositoryOwner, String repositoryName) {
            storage.values().removeIf(c -> c.getUserId().equals(userId)
                    && c.getRepositoryOwner().equalsIgnoreCase(repositoryOwner)
                    && c.getRepositoryName().equalsIgnoreCase(repositoryName));
        }

        @Override
        public List<GithubContribution> findDueContributions(LocalDateTime now, Pageable pageable) {
            return storage.values().stream()
                    .filter(c -> c.isSelected() && (c.getNextSyncAt() == null || !c.getNextSyncAt().isAfter(now)))
                    .limit(pageable.getPageSize())
                    .toList();
        }

        @Override
        public <S extends GithubContribution> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override public Optional<GithubContribution> findById(UUID uuid) { return Optional.ofNullable(storage.get(uuid)); }
        @Override public boolean existsById(UUID uuid) { return storage.containsKey(uuid); }
        @Override public List<GithubContribution> findAll() { return new ArrayList<>(storage.values()); }
        @Override public List<GithubContribution> findAllById(Iterable<UUID> uuids) { return List.of(); }
        @Override public long count() { return storage.size(); }
        @Override public void deleteById(UUID uuid) { storage.remove(uuid); }
        @Override public void delete(GithubContribution entity) { storage.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends UUID> uuids) {}
        @Override public void deleteAll(Iterable<? extends GithubContribution> entities) {}
        @Override public void deleteAll() { storage.clear(); }
        @Override public void flush() {}
        @Override public <S extends GithubContribution> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends GithubContribution> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<GithubContribution> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
        @Override public void deleteAllInBatch() {}
        @Override public GithubContribution getOne(UUID uuid) { return storage.get(uuid); }
        @Override public GithubContribution getById(UUID uuid) { return storage.get(uuid); }
        @Override public GithubContribution getReferenceById(UUID uuid) { return storage.get(uuid); }
        @Override public <S extends GithubContribution> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends GithubContribution> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override public <S extends GithubContribution> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public <S extends GithubContribution> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends GithubContribution> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends GithubContribution> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends GithubContribution, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends GithubContribution> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public List<GithubContribution> findAll(org.springframework.data.domain.Sort sort) { return List.of(); }
        @Override public org.springframework.data.domain.Page<GithubContribution> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }
}
