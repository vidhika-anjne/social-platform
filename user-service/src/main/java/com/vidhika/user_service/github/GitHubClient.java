package com.vidhika.user_service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.user_service.dto.GithubRepoItem;
import com.vidhika.user_service.exception.*;
import com.vidhika.user_service.github.dto.ContributionRepositoriesPage;
import com.vidhika.user_service.github.dto.GraphQLRequest;
import com.vidhika.user_service.github.dto.RepositoryContributionStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Low-level HTTP client responsible solely for communicating with the GitHub API
 * (both GraphQL and lightweight REST endpoints).
 */
@Slf4j
@Component
public class GitHubClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String defaultGithubToken;

    public GitHubClient(
            @Value("${github.api.url:https://api.github.com}") String githubApiUrl,
            @Value("${github.token:}") String defaultGithubToken,
            ObjectMapper objectMapper) {
        this.defaultGithubToken = defaultGithubToken;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(githubApiUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "Pulse-App")
                .build();
    }

    /**
     * Executes a GraphQL query against GitHub's POST /graphql endpoint.
     *
     * @param query     GraphQL query string
     * @param variables Query variables map
     * @param token     User access token (if null, falls back to default system token if present)
     * @return data node of the GraphQL response
     */
    public JsonNode executeGraphQLQuery(String query, Map<String, Object> variables, String token) {
        String effectiveToken = (token != null && !token.isBlank()) ? token : defaultGithubToken;

        GraphQLRequest requestBody = GraphQLRequest.builder()
                .query(query)
                .variables(variables != null ? variables : Map.of())
                .build();

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri("/graphql")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (effectiveToken != null && !effectiveToken.isBlank()) {
                            headers.setBearerAuth(effectiveToken);
                        }
                    })
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.value() == 401, (req, res) -> {
                        throw new GitHubAuthenticationException("GitHub API 401 Unauthorized: Invalid or missing GitHub token.");
                    })
                    .onStatus(status -> status.value() == 403, (req, res) -> {
                        String rateLimitRemaining = res.getHeaders().getFirst("X-RateLimit-Remaining");
                        if ("0".equals(rateLimitRemaining)) {
                            throw new GitHubRateLimitException("GitHub API rate limit exceeded (X-RateLimit-Remaining is 0).");
                        }
                        throw new GitHubApiException("GitHub API 403 Forbidden.");
                    })
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new GitHubResourceNotFoundException("GitHub endpoint or resource not found (404).");
                    })
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new GitHubApiException("GitHub API error with status: " + res.getStatusCode());
                    })
                    .toEntity(String.class);

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new GitHubApiException("Empty response received from GitHub GraphQL API");
            }

            JsonNode root = objectMapper.readTree(responseBody);

            // Parse GraphQL errors if present
            if (root.has("errors") && !root.get("errors").isEmpty()) {
                JsonNode errors = root.get("errors");
                for (JsonNode error : errors) {
                    String message = error.path("message").asText();
                    String type = error.path("type").asText();

                    if ("NOT_FOUND".equalsIgnoreCase(type) ||
                            (message != null && message.toLowerCase().contains("could not resolve to a repository"))) {
                        throw new RepositoryNotFoundException(message != null ? message : "Repository not found on GitHub.");
                    }
                    if (message != null && message.toLowerCase().contains("rate limit")) {
                        throw new GitHubRateLimitException("GitHub rate limit reached: " + message);
                    }
                    if (message != null && (message.toLowerCase().contains("bad credentials") || message.toLowerCase().contains("personal access token"))) {
                        throw new GitHubAuthenticationException("GitHub authentication failure: " + message);
                    }
                }

                // If data is absent or null and error wasn't handled above, fail fast
                if (!root.has("data") || root.get("data").isNull()) {
                    throw new GitHubApiException("GitHub GraphQL query failed: " + errors.get(0).path("message").asText());
                }
            }

            return root.path("data");

        } catch (GitHubAuthenticationException | GitHubRateLimitException | RepositoryNotFoundException
                 | GitHubResourceNotFoundException | GitHubApiException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.error("Network error communicating with GitHub API: {}", e.getMessage());
            throw new GitHubApiException("Network failure communicating with GitHub API: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.error("RestClientException calling GitHub GraphQL: {}", e.getMessage());
            throw new GitHubApiException("Error calling GitHub GraphQL API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error parsing GitHub response: {}", e.getMessage(), e);
            throw new GitHubApiException("Unexpected error processing GitHub response: " + e.getMessage(), e);
        }
    }

    /**
     * Part 1: GitHub contribution discovery.
     *
     * Queries GitHub GraphQL API for repositories the user contributed to.
     *
     * LIMITATION NOTE ON ORDERING:
     * The GitHub GraphQL schema's 'repositoriesContributedTo' connection only accepts sorting via
     * RepositoryOrderField (CREATED_AT, UPDATED_AT, PUSHED_AT, NAME, STARGAZERS). It does NOT
     * support sorting by the exact timestamp of this specific user's contribution (no CONTRIBUTED_AT
     * field exists in the schema).
     * We order by { field: PUSHED_AT, direction: DESC } as the closest reliable indicator of recent
     * activity in contributed repositories, while filtering includeUserRepositories: false to focus
     * on open-source contributions.
     *
     * @param token    User's OAuth token (or fallback)
     * @param username GitHub username of the user
     * @param first    Max count (defaults to 10)
     * @param after    Cursor for pagination
     * @return ContributionRepositoriesPage containing lightweight repository list and pagination info
     */
    public ContributionRepositoriesPage getContributionRepositories(String token, String username, int first, String after) {
        String query = """
                query GetContributionRepositories($username: String!, $first: Int!, $after: String) {
                  user(login: $username) {
                    repositoriesContributedTo(
                      first: $first
                      after: $after
                      orderBy: { field: PUSHED_AT, direction: DESC }
                      includeUserRepositories: false
                      contributionTypes: [COMMIT, ISSUE, PULL_REQUEST, PULL_REQUEST_REVIEW]
                    ) {
                      totalCount
                      pageInfo {
                        hasNextPage
                        endCursor
                      }
                      nodes {
                        id
                        name
                        nameWithOwner
                        url
                        owner {
                          login
                        }
                      }
                    }
                  }
                }
                """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("first", first > 0 ? first : 10);
        if (after != null && !after.isBlank()) {
            variables.put("after", after);
        }

        JsonNode data = executeGraphQLQuery(query, variables, token);
        JsonNode userNode = data.path("user");
        if (userNode.isNull() || userNode.isMissingNode()) {
            throw new GitHubResourceNotFoundException("GitHub user '" + username + "' not found.");
        }

        JsonNode repoConnection = userNode.path("repositoriesContributedTo");
        JsonNode nodes = repoConnection.path("nodes");
        List<GithubRepoItem> repos = new ArrayList<>();

        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                if (node.isNull() || node.isMissingNode()) continue;

                repos.add(GithubRepoItem.builder()
                        .repositoryId(node.path("id").asText())
                        .name(node.path("name").asText())
                        .nameWithOwner(node.path("nameWithOwner").asText())
                        .url(node.path("url").asText())
                        .owner(node.path("owner").path("login").asText())
                        .build());
            }
        }

        JsonNode pageInfo = repoConnection.path("pageInfo");
        return ContributionRepositoriesPage.builder()
                .repositories(repos)
                .hasNextPage(pageInfo.path("hasNextPage").asBoolean(false))
                .endCursor(pageInfo.path("endCursor").asText(null))
                .totalCount(repoConnection.path("totalCount").asInt(0))
                .build();
    }

    /**
     * Part 2 & Part 4: Detailed contribution statistics and verification.
     *
     * Uses GitHub GraphQL to verify repository existence and calculate pullRequestsCreated
     * and pullRequestsMerged in a single lightweight round-trip without downloading PR bodies/files.
     */
    public RepositoryContributionStats getRepositoryContributionStats(
            String token, String username, String owner, String repo) {

        String query = """
                query VerifyAndGetPRStats($owner: String!, $repo: String!, $createdQuery: String!, $mergedQuery: String!) {
                  repository(owner: $owner, name: $repo) {
                    id
                    name
                    nameWithOwner
                    url
                  }
                  createdPRs: search(query: $createdQuery, type: ISSUE, first: 1) {
                    issueCount
                  }
                  mergedPRs: search(query: $mergedQuery, type: ISSUE, first: 1) {
                    issueCount
                  }
                }
                """;

        String createdQuery = "repo:" + owner + "/" + repo + " is:pr author:" + username;
        String mergedQuery = "repo:" + owner + "/" + repo + " is:pr is:merged author:" + username;

        Map<String, Object> variables = Map.of(
                "owner", owner,
                "repo", repo,
                "createdQuery", createdQuery,
                "mergedQuery", mergedQuery
        );

        JsonNode data = executeGraphQLQuery(query, variables, token);

        JsonNode repoNode = data.path("repository");
        if (repoNode.isNull() || repoNode.isMissingNode()) {
            throw new RepositoryNotFoundException("Repository '" + owner + "/" + repo + "' not found on GitHub.");
        }

        String repoId = repoNode.path("id").asText();
        String repoName = repoNode.path("name").asText();
        String nameWithOwner = repoNode.path("nameWithOwner").asText();
        String repoUrl = repoNode.path("url").asText();

        int prsCreated = data.path("createdPRs").path("issueCount").asInt(0);
        int prsMerged = data.path("mergedPRs").path("issueCount").asInt(0);

        boolean verified = (prsCreated > 0);

        // If no PRs created by this user, check if they contributed direct commits
        if (!verified) {
            verified = checkUserHasCommits(owner, repo, username, token);
        }

        return RepositoryContributionStats.builder()
                .repositoryId(repoId)
                .repositoryName(repoName)
                .repositoryOwner(owner)
                .nameWithOwner(nameWithOwner)
                .repositoryUrl(repoUrl)
                .verified(verified)
                .pullRequestsCreated(prsCreated)
                .pullRequestsMerged(prsMerged)
                .build();
    }

    /**
     * Checks if a user has contributed commits to a repository via GitHub REST endpoint.
     */
    public boolean checkUserHasCommits(String owner, String repo, String username, String token) {
        String effectiveToken = (token != null && !token.isBlank()) ? token : defaultGithubToken;
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri("/repos/{owner}/{repo}/commits?author={username}&per_page=1", owner, repo, username)
                    .headers(headers -> {
                        if (effectiveToken != null && !effectiveToken.isBlank()) {
                            headers.setBearerAuth(effectiveToken);
                        }
                    })
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode commits = objectMapper.readTree(response.getBody());
                return commits.isArray() && !commits.isEmpty();
            }
        } catch (Exception e) {
            log.debug("Commit check fallback encountered: {}", e.getMessage());
        }
        return false;
    }
}
