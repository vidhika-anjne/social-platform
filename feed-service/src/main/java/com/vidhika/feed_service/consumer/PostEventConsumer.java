package com.vidhika.feed_service.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.feed_service.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventConsumer {

    private final FeedService feedService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "post-events", groupId = "feed-service-group")
    public void consumePostEvent(String message) {
        log.info("Received Kafka event in feed-service: {}", message);

        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();

            if ("POST_CREATED".equals(eventType)) {
                String postIdStr = root.path("postId").asText();
                String authorIdStr = root.path("authorId").asText();

                if (authorIdStr == null || authorIdStr.isBlank()) {
                    authorIdStr = root.path("userId").asText();
                }

                UUID postId = UUID.fromString(postIdStr);
                UUID authorId = UUID.fromString(authorIdStr);

                LocalDateTime createdAt = null;
                String createdAtStr = root.path("createdAt").asText(null);
                if (createdAtStr != null && !createdAtStr.isBlank()) {
                    try {
                        createdAt = LocalDateTime.parse(createdAtStr);
                    } catch (Exception ignored) {
                    }
                }

                feedService.processPostCreatedEvent(authorId, postId, createdAt);
            } else {
                log.info("Ignored eventType in feed-service: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message in feed-service: {}", message, e);
        }
    }
}
