package com.vidhika.user_service.controller;

import com.vidhika.user_service.dto.*;
import com.vidhika.user_service.github.GitHubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class GitHubContributionController {

    private final GitHubService gitHubService;

    /**
     * Part 1: GitHub Contribution Discovery.
     * Returns lightweight list of initial 10 recent/relevant contribution repositories.
     */
    @GetMapping("/me/github/contributions/repositories")
    public ResponseEntity<List<GithubRepoItem>> discoverContributions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "first", defaultValue = "10") int first,
            @RequestParam(name = "after", required = false) String after) {

        UUID userId = UUID.fromString(jwt.getSubject());
        List<GithubRepoItem> repos = gitHubService.discoverContributionRepositories(userId, first, after);
        return ResponseEntity.ok(repos);
    }

    /**
     * Part 2 & Part 4: Detailed Contribution Statistics & Verification for a Specific Repository.
     * Can be used for discovered repositories or manually supplied {owner}/{repo}.
     */
    @GetMapping("/me/github/contributions/{owner}/{repo}")
    public ResponseEntity<GithubRepoContributionDetailResponse> getContributionDetails(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repo) {

        UUID userId = UUID.fromString(jwt.getSubject());
        GithubRepoContributionDetailResponse response = gitHubService.getDetailedContributionStats(userId, owner, repo);
        return ResponseEntity.ok(response);
    }

    /**
     * Part 3: Selecting a Repository for the Pulse Profile.
     * Verifies contribution -> fetches PR stats -> saves/updates github_contributions table.
     */
    @PostMapping("/me/github/contributions/select")
    public ResponseEntity<GithubContributionResponse> selectRepository(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SelectRepositoryRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        GithubContributionResponse response = gitHubService.verifyAndSaveRepositoryContribution(
                userId, request.getOwner(), request.getRepo());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Part 3: Reads selected repositories for authenticated user's profile from database.
     * Zero GitHub API calls.
     */
    @GetMapping("/me/github/contributions/selected")
    public ResponseEntity<List<GithubContributionResponse>> getMySelectedContributions(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        List<GithubContributionResponse> response = gitHubService.getSelectedContributions(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Part 3: Public profile view of selected GitHub contributions for any user.
     * Reads directly from database.
     */
    @GetMapping("/{userId}/github/contributions")
    public ResponseEntity<List<GithubContributionResponse>> getUserSelectedContributions(
            @PathVariable("userId") UUID userId) {

        List<GithubContributionResponse> response = gitHubService.getSelectedContributions(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Unselect / remove a repository from display on Pulse profile.
     */
    @DeleteMapping("/me/github/contributions/{owner}/{repo}")
    public ResponseEntity<Void> unselectRepository(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("owner") String owner,
            @PathVariable("repo") String repo) {

        UUID userId = UUID.fromString(jwt.getSubject());
        gitHubService.unselectContribution(userId, owner, repo);
        return ResponseEntity.noContent().build();
    }

    /**
     * Connect or update connected GitHub account details for the authenticated user.
     */
    @PostMapping("/me/github/connect")
    public ResponseEntity<Void> connectGithub(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConnectGithubRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        gitHubService.connectGithubAccount(userId, request);
        return ResponseEntity.ok().build();
    }
}
