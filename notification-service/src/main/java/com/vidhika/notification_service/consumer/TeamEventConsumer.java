package com.vidhika.notification_service.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.notification_service.dto.kafka.HackathonStatusUpdatedEvent;
import com.vidhika.notification_service.dto.kafka.TeamFormedEvent;
import com.vidhika.notification_service.model.Notification;
import com.vidhika.notification_service.model.NotificationPreference;
import com.vidhika.notification_service.repository.NotificationPreferenceRepository;
import com.vidhika.notification_service.repository.NotificationRepository;
import com.vidhika.notification_service.service.NovuNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Kafka consumer for the team-events topic.
 * Listens for team and hackathon lifecycle events: TEAM_FORMED and HACKATHON_REGISTRATION_STATUS_UPDATED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TeamEventConsumer {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NovuNotificationService novuNotificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topic.team-events:team-events}", groupId = "notification-team-group")
    public void consumeTeamEvent(String message) {
        log.info("Received Kafka event from team-events topic: {}", message);

        try {
            JsonNode root = objectMapper.readTree(message);
            String eventType = root.path("eventType").asText(null);

            if (eventType == null && root.has("payload")) {
                JsonNode payloadNode = root.path("payload");
                eventType = payloadNode.path("eventType").asText(null);
            }

            if ("TEAM_FORMED".equalsIgnoreCase(eventType)) {
                TeamFormedEvent event = objectMapper.treeToValue(root, TeamFormedEvent.class);
                handleTeamFormed(event);
            } else if ("HACKATHON_REGISTRATION_STATUS_UPDATED".equalsIgnoreCase(eventType)) {
                HackathonStatusUpdatedEvent event = objectMapper.treeToValue(root, HackathonStatusUpdatedEvent.class);
                handleStatusUpdated(event);
            } else {
                log.info("Unhandled eventType from team-events: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Kafka message from team-events: {}", message, e);
        }
    }

    private void handleTeamFormed(TeamFormedEvent event) {
        if (event.getMemberIds() == null || event.getMemberIds().isEmpty()) {
            log.warn("TEAM_FORMED event has no memberIds: {}", event);
            return;
        }

        String teamName = (event.getTeamName() != null && !event.getTeamName().isBlank())
                ? event.getTeamName()
                : "your team";
        String hackathonTitle = (event.getHackathonTitle() != null && !event.getHackathonTitle().isBlank())
                ? event.getHackathonTitle()
                : "the hackathon";
        String notifMsg = "Your team '" + teamName + "' has formed for " + hackathonTitle + "!";

        UUID hackathonUUID = null;
        if (event.getHackathonId() != null && !event.getHackathonId().isBlank()) {
            try {
                hackathonUUID = UUID.fromString(event.getHackathonId());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid hackathon UUID in TEAM_FORMED event: {}", event.getHackathonId());
            }
        }

        for (String memberIdStr : event.getMemberIds()) {
            try {
                UUID recipientId = UUID.fromString(memberIdStr.trim());

                // 1. Persist notification in database
                Notification notification = Notification.builder()
                        .recipientId(recipientId)
                        .type("TEAM_FORMED")
                        .hackathonId(hackathonUUID)
                        .message(notifMsg)
                        .read(false)
                        .build();

                notificationRepository.save(notification);

                // 2. Check preferences & DND filter
                Optional<NotificationPreference> optPref = preferenceRepository.findByUserId(recipientId);
                boolean pushEnabled = optPref.map(NotificationPreference::isPushEnabled).orElse(true);
                boolean isDndActive = optPref.map(p -> p.isDndActiveNow(LocalTime.now())).orElse(false);

                if (!pushEnabled || isDndActive) {
                    log.info("Push suppressed (pushEnabled={}, isDndActive={}) for recipient {}",
                            pushEnabled, isDndActive, recipientId);
                    continue;
                }

                // 3. Dispatch to Novu
                Map<String, Object> payload = new HashMap<>();
                payload.put("teamId", event.getTeamId() != null ? event.getTeamId() : "");
                payload.put("teamName", teamName);
                payload.put("hackathonId", event.getHackathonId() != null ? event.getHackathonId() : "");
                payload.put("hackathonTitle", hackathonTitle);
                payload.put("message", notifMsg);

                novuNotificationService.trigger("team-formed", recipientId, payload);
                log.info("Dispatched TEAM_FORMED notification for user {} in team {}", recipientId, teamName);

            } catch (Exception e) {
                log.error("Failed to process TEAM_FORMED notification for member {}: {}", memberIdStr, e.getMessage(), e);
            }
        }
    }

    private void handleStatusUpdated(HackathonStatusUpdatedEvent event) {
        if (event.getRecipientIds() == null || event.getRecipientIds().isEmpty()) {
            log.warn("HACKATHON_REGISTRATION_STATUS_UPDATED event has no recipientIds: {}", event);
            return;
        }

        String status = event.getStatus() != null ? event.getStatus() : "UPDATED";
        String title = (event.getHackathonTitle() != null && !event.getHackathonTitle().isBlank())
                ? event.getHackathonTitle()
                : "the hackathon";
        String subject = (event.getTeamName() != null && !event.getTeamName().isBlank())
                ? "Your team '" + event.getTeamName() + "'"
                : "Your registration";

        String message;
        if ("WINNER".equalsIgnoreCase(status)) {
            message = "🏆 Congratulations! " + subject + " won 1st Place in " + title + "!";
        } else if ("RUNNER_UP".equalsIgnoreCase(status)) {
            message = "🥈 Congratulations! " + subject + " placed as Runner-Up in " + title + "!";
        } else if ("SHORTLISTED".equalsIgnoreCase(status)) {
            message = "🎉 Congratulations! " + subject + " was shortlisted for " + title + "!";
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            message = "Update on " + title + ": " + subject + " was not selected.";
        } else {
            message = "Update on " + title + ": " + subject + " status changed to " + status + ".";
        }

        UUID hackathonUUID = null;
        if (event.getHackathonId() != null && !event.getHackathonId().isBlank()) {
            try {
                hackathonUUID = UUID.fromString(event.getHackathonId());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid hackathon UUID in status update event: {}", event.getHackathonId());
            }
        }

        for (String recipientIdStr : event.getRecipientIds()) {
            try {
                UUID recipientId = UUID.fromString(recipientIdStr.trim());

                // 1. Persist notification in database
                Notification notification = Notification.builder()
                        .recipientId(recipientId)
                        .type("HACKATHON_STATUS_UPDATED")
                        .hackathonId(hackathonUUID)
                        .message(message)
                        .read(false)
                        .build();

                notificationRepository.save(notification);

                // 2. Check preferences & DND filter
                Optional<NotificationPreference> optPref = preferenceRepository.findByUserId(recipientId);
                boolean pushEnabled = optPref.map(NotificationPreference::isPushEnabled).orElse(true);
                boolean isDndActive = optPref.map(p -> p.isDndActiveNow(LocalTime.now())).orElse(false);

                if (!pushEnabled || isDndActive) {
                    log.info("Push suppressed (pushEnabled={}, isDndActive={}) for recipient {}",
                            pushEnabled, isDndActive, recipientId);
                    continue;
                }

                // 3. Dispatch to Novu
                Map<String, Object> payload = new HashMap<>();
                payload.put("hackathonId", event.getHackathonId() != null ? event.getHackathonId() : "");
                payload.put("hackathonTitle", title);
                payload.put("status", status);
                payload.put("teamName", event.getTeamName() != null ? event.getTeamName() : "");
                payload.put("message", message);

                novuNotificationService.trigger("hackathon-status-updated", recipientId, payload);
                log.info("Dispatched HACKATHON_STATUS_UPDATED notification to user {} (status: {})", recipientId, status);

            } catch (Exception e) {
                log.error("Failed to process status update notification for recipient {}: {}", recipientIdStr, e.getMessage(), e);
            }
        }
    }
}
