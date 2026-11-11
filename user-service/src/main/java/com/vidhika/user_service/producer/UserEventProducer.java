package com.vidhika.user_service.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.user_service.dto.kafka.UserFollowedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.user-events:user-events}")
    private String userEventsTopic;

    public void publishUserFollowedEvent(UserFollowedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event.getTargetUserId() != null ? event.getTargetUserId().toString() : "";
            kafkaTemplate.send(userEventsTopic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish USER_FOLLOWED event for target user {}: {}",
                                    event.getTargetUserId(), ex.getMessage());
                        } else {
                            log.info("Published USER_FOLLOWED event: follower {} -> target {}",
                                    event.getFollowerId(), event.getTargetUserId());
                        }
                    });
        } catch (Exception e) {
            log.error("Error serializing USER_FOLLOWED event for target user {}: {}",
                    event.getTargetUserId(), e.getMessage(), e);
        }
    }
}
