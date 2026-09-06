package com.vidhika.notificaiton_service.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.notificaiton_service.service.NotificationAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka consumer for the post-events topic.
 * Responsibility: parse incoming events and delegate to the aggregation service.
 * Does NOT create notifications directly — that is the scheduler's job.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventConsumer {

    private final NotificationAggregationService aggregationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "post-events", groupId = "notification-service-group")
    public void consumePostEvent(String message) {
        log.info("Received Kafka event from post-events topic: {}", message);

        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText();

            if ("POST_LIKED".equals(eventType)) {
                handlePostLikedEvent(root);
            } else {
                log.info("Unhandled eventType from post-events: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message from post-events: {}", message, e);
        }
    }

    private void handlePostLikedEvent(JsonNode root) {
        UUID postId = UUID.fromString(root.path("postId").asText());
        UUID likerId = UUID.fromString(root.path("likerId").asText());
        UUID postOwnerId = UUID.fromString(root.path("postOwnerId").asText());

        // Don't notify if user liked their own post
        if (likerId.equals(postOwnerId)) {
            log.info("Skipping self-like notification for postId={}, userId={}", postId, likerId);
            return;
        }

        // recipientId = postOwnerId (the person who should be notified)
        aggregationService.recordLikeEvent(postOwnerId, postId, likerId);
    }
}
