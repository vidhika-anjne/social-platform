package com.vidhika.chat_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Client to verify user relationship checks (blocking, follows, privacy) from user-service.
 */
@Slf4j
@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Value("${user-service.url:http://localhost:8083}") String userServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    /**
     * Checks if either user has blocked the other.
     */
    public boolean isBlocked(String userA, String userB) {
        try {
            Map<String, Boolean> response = restClient.get()
                    .uri("/api/v1/users/{userId}/is-blocked/{targetUserId}", userA, userB)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Boolean>>() {});
            return response != null && Boolean.TRUE.equals(response.get("blocked"));
        } catch (Exception e) {
            log.warn("Failed to check block status between {} and {}: {}", userA, userB, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if followerId follows targetUserId.
     */
    public boolean isFollowing(String followerId, String targetUserId) {
        try {
            Map<String, Boolean> response = restClient.get()
                    .uri("/api/v1/users/{userId}/is-following/{targetUserId}", followerId, targetUserId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Boolean>>() {});
            return response != null && Boolean.TRUE.equals(response.get("following"));
        } catch (Exception e) {
            log.warn("Failed to check follow status between {} and {}: {}", followerId, targetUserId, e.getMessage());
            return false;
        }
    }
}

