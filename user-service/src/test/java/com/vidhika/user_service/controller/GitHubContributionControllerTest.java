package com.vidhika.user_service.controller;

import com.vidhika.user_service.dto.*;
import com.vidhika.user_service.github.GitHubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GitHubContributionControllerTest {

    private StubGitHubService gitHubService;
    private GitHubContributionController controller;
    private Jwt mockJwt;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        gitHubService = new StubGitHubService();
        controller = new GitHubContributionController(gitHubService);

        mockJwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS384"),
                Map.of("sub", userId.toString(), "email", "test@pulse.com")
        );
    }

    @Test
    void discoverContributions_returnsRepositoryList() {
        gitHubService.discoveredRepos = List.of(
                GithubRepoItem.builder()
                        .repositoryId("R_123")
                        .name("react")
                        .nameWithOwner("facebook/react")
                        .url("https://github.com/facebook/react")
                        .owner("facebook")
                        .build()
        );

        ResponseEntity<List<GithubRepoItem>> response =
                controller.discoverContributions(mockJwt, 10, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("react", response.getBody().get(0).getName());
        assertEquals(userId, gitHubService.lastUserId);
        assertEquals(10, gitHubService.lastFirst);
    }

    @Test
    void getContributionDetails_returnsDetailResponse() {
        gitHubService.detailResponse = GithubRepoContributionDetailResponse.builder()
                .repositoryId("R_123")
                .nameWithOwner("facebook/react")
                .repositoryUrl("https://github.com/facebook/react")
                .verified(true)
                .pullRequestsCreated(12)
                .pullRequestsMerged(9)
                .build();

        ResponseEntity<GithubRepoContributionDetailResponse> response =
                controller.getContributionDetails(mockJwt, "facebook", "react");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isVerified());
        assertEquals(12, response.getBody().getPullRequestsCreated());
        assertEquals(9, response.getBody().getPullRequestsMerged());
    }

    @Test
    void selectRepository_returnsCreatedContribution() {
        gitHubService.savedResponse = GithubContributionResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .githubRepositoryId("R_123")
                .repositoryName("react")
                .repositoryOwner("facebook")
                .repositoryUrl("https://github.com/facebook/react")
                .pullRequestsCreated(12)
                .pullRequestsMerged(9)
                .selected(true)
                .lastSyncedAt(LocalDateTime.now())
                .build();

        SelectRepositoryRequest request = SelectRepositoryRequest.builder()
                .owner("facebook")
                .repo("react")
                .build();

        ResponseEntity<GithubContributionResponse> response =
                controller.selectRepository(mockJwt, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("react", response.getBody().getRepositoryName());
        assertTrue(response.getBody().isSelected());
    }

    @Test
    void getMySelectedContributions_returnsSavedContributions() {
        gitHubService.selectedList = List.of(
                GithubContributionResponse.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .githubRepositoryId("R_123")
                        .repositoryName("react")
                        .repositoryOwner("facebook")
                        .pullRequestsCreated(5)
                        .pullRequestsMerged(4)
                        .selected(true)
                        .build()
        );

        ResponseEntity<List<GithubContributionResponse>> response =
                controller.getMySelectedContributions(mockJwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("react", response.getBody().get(0).getRepositoryName());
    }

    @Test
    void getUserSelectedContributions_publicEndpoint_returnsList() {
        UUID otherUserId = UUID.randomUUID();
        gitHubService.selectedList = List.of(
                GithubContributionResponse.builder()
                        .id(UUID.randomUUID())
                        .userId(otherUserId)
                        .githubRepositoryId("R_456")
                        .repositoryName("vscode")
                        .repositoryOwner("microsoft")
                        .pullRequestsCreated(2)
                        .pullRequestsMerged(2)
                        .selected(true)
                        .build()
        );

        ResponseEntity<List<GithubContributionResponse>> response =
                controller.getUserSelectedContributions(otherUserId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("vscode", response.getBody().get(0).getRepositoryName());
    }

    @Test
    void unselectRepository_returnsNoContent() {
        ResponseEntity<Void> response = controller.unselectRepository(mockJwt, "facebook", "react");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals("facebook", gitHubService.lastUnselectOwner);
        assertEquals("react", gitHubService.lastUnselectRepo);
    }

    @Test
    void connectGithub_returnsOk() {
        ConnectGithubRequest request = ConnectGithubRequest.builder()
                .githubUsername("octocat")
                .githubAccessToken("ghp_test")
                .build();

        ResponseEntity<Void> response = controller.connectGithub(mockJwt, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("octocat", gitHubService.lastConnectRequest.getGithubUsername());
    }

    private static class StubGitHubService extends GitHubService {
        List<GithubRepoItem> discoveredRepos = List.of();
        GithubRepoContributionDetailResponse detailResponse;
        GithubContributionResponse savedResponse;
        List<GithubContributionResponse> selectedList = List.of();

        UUID lastUserId;
        int lastFirst;
        String lastUnselectOwner;
        String lastUnselectRepo;
        ConnectGithubRequest lastConnectRequest;

        StubGitHubService() {
            super(null, null, null);
        }

        @Override
        public List<GithubRepoItem> discoverContributionRepositories(UUID userId, int first, String after) {
            this.lastUserId = userId;
            this.lastFirst = first;
            return discoveredRepos;
        }

        @Override
        public GithubRepoContributionDetailResponse getDetailedContributionStats(UUID userId, String owner, String repo) {
            return detailResponse;
        }

        @Override
        public GithubContributionResponse verifyAndSaveRepositoryContribution(UUID userId, String owner, String repo) {
            return savedResponse;
        }

        @Override
        public List<GithubContributionResponse> getSelectedContributions(UUID userId) {
            return selectedList;
        }

        @Override
        public void unselectContribution(UUID userId, String owner, String repo) {
            this.lastUnselectOwner = owner;
            this.lastUnselectRepo = repo;
        }

        @Override
        public void connectGithubAccount(UUID userId, ConnectGithubRequest request) {
            this.lastConnectRequest = request;
        }
    }
}
