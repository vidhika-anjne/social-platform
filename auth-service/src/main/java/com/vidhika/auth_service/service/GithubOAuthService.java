package com.vidhika.auth_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vidhika.auth_service.dto.GithubUserResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Getter
public class GithubOAuthService {

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    @Value("${github.redirect-uri:http://localhost:8080/api/v1/auth/github/callback}")
    private String redirectUri;

    private final RestClient restClient;

    public GithubOAuthService() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, "Pulse-App")
                .build();
    }

    /**
     * Builds GitHub OAuth authorization URL to redirect user in browser.
     */
    public String getAuthorizationUrl() {
        return "https://github.com/login/oauth/authorize?client_id=" + clientId
                + "&scope=user:email"
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    }

    /**
     * Exchanges temporary authorization code for GitHub access token.
     */
    public String getAccessToken(String code) {
        log.info("Exchanging authorization code with GitHub for access token...");

        Map<String, String> payload = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri
        );

        JsonNode response = restClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new RuntimeException("Null response received from GitHub OAuth access token endpoint");
        }

        if (response.has("error")) {
            String errorMsg = response.has("error_description")
                    ? response.get("error_description").asText()
                    : response.get("error").asText();
            log.error("GitHub OAuth error: {}", errorMsg);
            throw new RuntimeException("GitHub OAuth failed: " + errorMsg);
        }

        if (!response.has("access_token")) {
            throw new RuntimeException("No access_token field present in GitHub response");
        }

        return response.get("access_token").asText();
    }

    /**
     * Fetches user profile from GitHub and ensures email is populated.
     */
    public GithubUserResponse getUserProfile(String accessToken) {
        log.info("Fetching user profile from GitHub API...");

        GithubUserResponse profile = restClient.get()
                .uri("https://api.github.com/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(GithubUserResponse.class);

        if (profile == null) {
            throw new RuntimeException("Failed to fetch user profile from GitHub");
        }

        // If email is private on GitHub profile, query user emails API
        if (profile.getEmail() == null || profile.getEmail().isBlank()) {
            log.info("Public email is null, fetching verified emails from GitHub /user/emails...");
            String resolvedEmail = fetchPrimaryEmail(accessToken);
            profile.setEmail(resolvedEmail);
        }

        return profile;
    }

    /**
     * Resolves the primary verified email from GitHub's /user/emails endpoint.
     */
    private String fetchPrimaryEmail(String accessToken) {
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (emails != null && !emails.isEmpty()) {
                // 1. Look for primary & verified email
                for (Map<String, Object> emailObj : emails) {
                    Boolean primary = (Boolean) emailObj.get("primary");
                    Boolean verified = (Boolean) emailObj.get("verified");
                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                        return (String) emailObj.get("email");
                    }
                }
                // 2. Look for any verified email
                for (Map<String, Object> emailObj : emails) {
                    Boolean verified = (Boolean) emailObj.get("verified");
                    if (Boolean.TRUE.equals(verified)) {
                        return (String) emailObj.get("email");
                    }
                }
                // 3. Fallback to first available email
                return (String) emails.get(0).get("email");
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve emails from /user/emails: {}", e.getMessage());
        }

        return null;
    }
}
