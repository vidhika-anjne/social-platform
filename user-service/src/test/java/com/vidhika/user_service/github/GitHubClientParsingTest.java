package com.vidhika.user_service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.user_service.dto.GithubRepoItem;
import com.vidhika.user_service.exception.GitHubAuthenticationException;
import com.vidhika.user_service.exception.GitHubRateLimitException;
import com.vidhika.user_service.exception.RepositoryNotFoundException;
import com.vidhika.user_service.github.dto.ContributionRepositoriesPage;
import com.vidhika.user_service.github.dto.RepositoryContributionStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GitHubClientParsingTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void parseDiscoveryGraphQLResponse_success() throws Exception {
        String json = """
                {
                  "data": {
                    "user": {
                      "repositoriesContributedTo": {
                        "totalCount": 1,
                        "pageInfo": {
                          "hasNextPage": false,
                          "endCursor": "Y3Vyc29yOnYyOpHOADw="
                        },
                        "nodes": [
                          {
                            "id": "R_kgDOABCDEF",
                            "name": "react",
                            "nameWithOwner": "facebook/react",
                            "url": "https://github.com/facebook/react",
                            "owner": {
                              "login": "facebook"
                            }
                          }
                        ]
                      }
                    }
                  }
                }
                """;

        GitHubClient client = new GitHubClient("http://localhost", "", objectMapper) {
            @Override
            public JsonNode executeGraphQLQuery(String query, Map<String, Object> variables, String token) {
                try {
                    return objectMapper.readTree(json).path("data");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        ContributionRepositoriesPage page = client.getContributionRepositories(null, "octocat", 10, null);

        assertNotNull(page);
        assertEquals(1, page.getRepositories().size());
        GithubRepoItem item = page.getRepositories().get(0);
        assertEquals("R_kgDOABCDEF", item.getRepositoryId());
        assertEquals("react", item.getName());
        assertEquals("facebook/react", item.getNameWithOwner());
        assertEquals("https://github.com/facebook/react", item.getUrl());
        assertEquals("facebook", item.getOwner());
        assertFalse(page.isHasNextPage());
        assertEquals("Y3Vyc29yOnYyOpHOADw=", page.getEndCursor());
    }

    @Test
    void parseRepositoryStatsGraphQLResponse_success() throws Exception {
        String json = """
                {
                  "data": {
                    "repository": {
                      "id": "R_kgDO12345",
                      "name": "react",
                      "nameWithOwner": "facebook/react",
                      "url": "https://github.com/facebook/react"
                    },
                    "createdPRs": {
                      "issueCount": 12
                    },
                    "mergedPRs": {
                      "issueCount": 9
                    }
                  }
                }
                """;

        GitHubClient client = new GitHubClient("http://localhost", "", objectMapper) {
            @Override
            public JsonNode executeGraphQLQuery(String query, Map<String, Object> variables, String token) {
                try {
                    return objectMapper.readTree(json).path("data");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        RepositoryContributionStats stats = client.getRepositoryContributionStats(null, "octocat", "facebook", "react");

        assertNotNull(stats);
        assertEquals("R_kgDO12345", stats.getRepositoryId());
        assertEquals("facebook/react", stats.getNameWithOwner());
        assertEquals("https://github.com/facebook/react", stats.getRepositoryUrl());
        assertTrue(stats.isVerified());
        assertEquals(12, stats.getPullRequestsCreated());
        assertEquals(9, stats.getPullRequestsMerged());
    }

    @Test
    void parseRepositoryStats_repositoryNotFound_throwsRepositoryNotFoundException() {
        GitHubClient client = new GitHubClient("http://localhost", "", objectMapper) {
            @Override
            public JsonNode executeGraphQLQuery(String query, Map<String, Object> variables, String token) {
                throw new RepositoryNotFoundException("Could not resolve to a Repository with the name 'facebook/unknown-repo'.");
            }
        };

        assertThrows(RepositoryNotFoundException.class, () ->
                client.getRepositoryContributionStats(null, "octocat", "facebook", "unknown-repo"));
    }

    @Test
    void parseRepositoryStats_rateLimit_throwsGitHubRateLimitException() {
        GitHubClient client = new GitHubClient("http://localhost", "", objectMapper) {
            @Override
            public JsonNode executeGraphQLQuery(String query, Map<String, Object> variables, String token) {
                throw new GitHubRateLimitException("GitHub rate limit reached: API rate limit exceeded");
            }
        };

        assertThrows(GitHubRateLimitException.class, () ->
                client.getRepositoryContributionStats(null, "octocat", "facebook", "react"));
    }

    @Test
    void parseRepositoryStats_invalidToken_throwsGitHubAuthenticationException() {
        GitHubClient client = new GitHubClient("http://localhost", "", objectMapper) {
            @Override
            public JsonNode executeGraphQLQuery(String query, Map<String, Object> variables, String token) {
                throw new GitHubAuthenticationException("GitHub authentication failure: Bad credentials");
            }
        };

        assertThrows(GitHubAuthenticationException.class, () ->
                client.getRepositoryContributionStats(null, "octocat", "facebook", "react"));
    }
}
