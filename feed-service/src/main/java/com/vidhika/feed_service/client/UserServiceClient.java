package com.vidhika.feed_service.client;

import com.vidhika.feed_service.dto.FollowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient webClient;

    @Value("${user-service.url:http://localhost:8083}")
    private String userServiceUrl;

    public List<FollowResponse> getFollowers(UUID userId) {
        try {
            log.info("Calling user-service to fetch followers for userId: {}", userId);
            List<FollowResponse> followers = webClient.get()
                    .uri(userServiceUrl + "/api/v1/users/{userId}/followers", userId)
                    .retrieve()
                    .bodyToFlux(FollowResponse.class)
                    .collectList()
                    .block();

            return followers != null ? followers : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch followers for userId: {} from user-service: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
