package com.vidhika.user_service.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.user_service.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-events", groupId = "user-service-group")
    public void consumeUserEvent(String message) {
        log.info("Received Kafka event message in user-service: {}", message);

        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();

            if ("USER_REGISTERED".equals(eventType)) {
                String userIdStr = root.path("userId").asText();
                if (userIdStr == null || userIdStr.isBlank()) {
                    userIdStr = root.path("aggregateId").asText();
                }

                UUID userId = UUID.fromString(userIdStr);
                String email = root.path("email").asText(null);
                String name = root.path("name").asText(null);

                userProfileService.createUserProfileFromEvent(userId, email, name);
            } else {
                log.info("Unhandled eventType: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message: {}", message, e);
        }
    }
}
