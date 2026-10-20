package com.vidhika.user_service.github;

import com.vidhika.user_service.dto.*;
import com.vidhika.user_service.exception.GitHubNotConnectedException;
import com.vidhika.user_service.github.dto.ContributionRepositoriesPage;
import com.vidhika.user_service.github.dto.RepositoryContributionStats;
import com.vidhika.user_service.model.GithubContribution;
import com.vidhika.user_service.model.UserProfile;
import com.vidhika.user_service.repository.GithubContributionRepository;
import com.vidhika.user_service.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GitHubServiceTest {

    private StubGitHubClient gitHubClient;
    private InMemoryUserProfileRepository userProfileRepository;
    private InMemoryGithubContributionRepository githubContributionRepository;
    private GitHubService gitHubService;

    private UUID userId;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userProfile = UserProfile.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .githubUsername("octocat")
                .githubAccessToken("gho_test123")
                .build();

        gitHubClient = new StubGitHubClient();
        userProfileRepository = new InMemoryUserProfileRepository();
        githubContributionRepository = new InMemoryGithubContributionRepository();

        userProfileRepository.save(userProfile);
        gitHubService = new GitHubService(gitHubClient, userProfileRepository, githubContributionRepository);
    }

    @Test
    void discoverContributionRepositories_success() {
        List<GithubRepoItem> expectedRepos = List.of(
                GithubRepoItem.builder()
                        .repositoryId("R_123")
                        .name("react")
                        .nameWithOwner("facebook/react")
                        .url("https://github.com/facebook/react")
                        .owner("facebook")
                        .build()
        );

        gitHubClient.nextPage = ContributionRepositoriesPage.builder()
                .repositories(expectedRepos)
                .hasNextPage(false)
                .totalCount(1)
                .build();

        List<GithubRepoItem> result = gitHubService.discoverContributionRepositories(userId, 10, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("react", result.get(0).getName());
        assertEquals("facebook/react", result.get(0).getNameWithOwner());
        assertEquals("facebook", result.get(0).getOwner());
    }

    @Test
    void discoverContributionRepositories_notConnected_throwsException() {
        userProfile.setGithubUsername(null);
        userProfileRepository.save(userProfile);

        assertThrows(GitHubNotConnectedException.class, () ->
                gitHubService.discoverContributionRepositories(userId, 10, null));
    }

    @Test
    void getDetailedContributionStats_success() {
        gitHubClient.nextStats = RepositoryContributionStats.builder()
                .repositoryId("R_123")
                .repositoryName("react")
                .repositoryOwner("facebook")
                .nameWithOwner("facebook/react")
                .repositoryUrl("https://github.com/facebook/react")
                .verified(true)
                .pullRequestsCreated(5)
                .pullRequestsMerged(4)
                .build();

        GithubRepoContributionDetailResponse response =
                gitHubService.getDetailedContributionStats(userId, "facebook", "react");

        assertNotNull(response);
        assertEquals("R_123", response.getRepositoryId());
        assertEquals("facebook/react", response.getNameWithOwner());
        assertTrue(response.isVerified());
        assertEquals(5, response.getPullRequestsCreated());
        assertEquals(4, response.getPullRequestsMerged());
    }

    @Test
    void verifyAndSaveRepositoryContribution_verified_savesContribution() {
        gitHubClient.nextStats = RepositoryContributionStats.builder()
                .repositoryId("R_123")
                .repositoryName("react")
                .repositoryOwner("facebook")
                .nameWithOwner("facebook/react")
                .repositoryUrl("https://github.com/facebook/react")
                .verified(true)
                .pullRequestsCreated(12)
                .pullRequestsMerged(9)
                .build();

        GithubContributionResponse response =
                gitHubService.verifyAndSaveRepositoryContribution(userId, "facebook", "react");

        assertNotNull(response);
        assertEquals("R_123", response.getGithubRepositoryId());
        assertEquals("facebook", response.getRepositoryOwner());
        assertEquals("react", response.getRepositoryName());
        assertEquals(12, response.getPullRequestsCreated());
        assertEquals(9, response.getPullRequestsMerged());
        assertTrue(response.isSelected());

        // Verify stored in DB
        Optional<GithubContribution> found = githubContributionRepository.findByUserIdAndGithubRepositoryId(userId, "R_123");
        assertTrue(found.isPresent());
        assertEquals(12, found.get().getPullRequestsCreated());
    }

    @Test
    void verifyAndSaveRepositoryContribution_unverified_throwsIllegalArgumentException() {
        gitHubClient.nextStats = RepositoryContributionStats.builder()
                .repositoryId("R_999")
                .repositoryName("secret-repo")
                .repositoryOwner("facebook")
                .nameWithOwner("facebook/secret-repo")
                .verified(false)
                .pullRequestsCreated(0)
                .pullRequestsMerged(0)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                gitHubService.verifyAndSaveRepositoryContribution(userId, "facebook", "secret-repo"));

        assertTrue(githubContributionRepository.findAll().isEmpty());
    }

    @Test
    void getSelectedContributions_readsFromDatabaseOnly() {
        GithubContribution saved = GithubContribution.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .githubRepositoryId("R_123")
                .repositoryName("react")
                .repositoryOwner("facebook")
                .repositoryUrl("https://github.com/facebook/react")
                .pullRequestsCreated(10)
                .pullRequestsMerged(8)
                .selected(true)
                .createdAt(LocalDateTime.now())
                .build();

        githubContributionRepository.save(saved);

        List<GithubContributionResponse> results = gitHubService.getSelectedContributions(userId);

        assertEquals(1, results.size());
        assertEquals("react", results.get(0).getRepositoryName());
        assertEquals(10, results.get(0).getPullRequestsCreated());
    }

    // --- Lightweight Stubs for Testing without ByteBuddy limitations on Java 25 ---

    private static class StubGitHubClient extends GitHubClient {
        ContributionRepositoriesPage nextPage;
        RepositoryContributionStats nextStats;

        StubGitHubClient() {
            super("http://localhost", "", null);
        }

        @Override
        public ContributionRepositoriesPage getContributionRepositories(String token, String username, int first, String after) {
            return nextPage;
        }

        @Override
        public RepositoryContributionStats getRepositoryContributionStats(String token, String username, String owner, String repo) {
            return nextStats;
        }
    }

    private static class InMemoryUserProfileRepository implements UserProfileRepository {
        private final Map<UUID, UserProfile> storage = new HashMap<>();

        @Override
        public Optional<UserProfile> findById(UUID uuid) {
            return Optional.ofNullable(storage.get(uuid));
        }

        @Override
        public <S extends UserProfile> S save(S entity) {
            storage.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<UserProfile> findByEmail(String email) {
            return storage.values().stream()
                    .filter(u -> email != null && email.equalsIgnoreCase(u.getEmail()))
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return storage.values().stream()
                    .anyMatch(u -> email != null && email.equalsIgnoreCase(u.getEmail()));
        }

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
