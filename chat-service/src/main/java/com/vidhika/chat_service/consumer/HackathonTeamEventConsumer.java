package com.vidhika.chat_service.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.chat_service.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HackathonTeamEventConsumer {

    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topic.team-events:team-events}", groupId = "${spring.kafka.consumer.group-id:chat-service-group}")
    public void consumeTeamFormedEvent(String message) {
        log.info("Received Kafka event on team-events: {}", message);

        try {
            JsonNode root = objectMapper.readTree(message);

            // Determine if wrapped in eventType
            String eventType = root.path("eventType").asText(null);
            JsonNode payloadNode = root;

            if (root.has("payload")) {
                payloadNode = root.path("payload");
            } else if (root.has("data")) {
                payloadNode = root.path("data");
            }

            // Verify eventType if present
            if (eventType != null && !eventType.isBlank() && !"TEAM_FORMED".equalsIgnoreCase(eventType)) {
                log.info("Ignoring unhandled event type: {}", eventType);
                return;
            }

            String teamId = payloadNode.path("teamId").asText(null);
            if (teamId == null || teamId.isBlank()) {
                teamId = root.path("teamId").asText(null);
            }

            String hackathonId = payloadNode.path("hackathonId").asText(null);
            if (hackathonId == null || hackathonId.isBlank()) {
                hackathonId = root.path("hackathonId").asText(null);
            }

            String teamName = payloadNode.path("teamName").asText(null);
            if (teamName == null || teamName.isBlank()) {
                teamName = root.path("teamName").asText(null);
            }

            List<String> memberIds = new ArrayList<>();
            JsonNode memberIdsNode = payloadNode.has("memberIds") ? payloadNode.path("memberIds") : root.path("memberIds");
            if (memberIdsNode != null && memberIdsNode.isArray()) {
                for (JsonNode node : memberIdsNode) {
                    if (node != null && !node.asText().isBlank()) {
                        memberIds.add(node.asText().trim());
                    }
                }
            }

            if (teamId == null || teamId.isBlank()) {
                log.error("Invalid TEAM_FORMED event: Missing teamId. Message: {}", message);
                return;
            }

            log.info("Processing TEAM_FORMED event for teamId: {}, hackathonId: {}, members count: {}",
                    teamId, hackathonId, memberIds.size());

            // Delegate to idempotent conversation creation service
            conversationService.createHackathonTeamChat(teamId, hackathonId, memberIds, teamName);

        } catch (Exception e) {
            log.error("Error processing Kafka TEAM_FORMED event: {}", message, e);
        }
    }
}
