package com.vidhika.notification_service.service;

import com.vidhika.notification_service.config.NovuProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovuNotificationService {

    private final RestClient novuRestClient;
    private final NovuProperties properties;

    /**
     * Trigger a Novu notification workflow.
     *
     * @param workflowId   Identifier of the workflow created in Novu dashboard (e.g. "post-liked", "dm-received")
     * @param subscriberId UUID or unique ID of the recipient user
     * @param payload      Dynamic data map sent to the notification template
     */
    public void trigger(String workflowId, String subscriberId, Map<String, Object> payload) {
        if (!properties.isEnabled()) {
            log.debug("Novu is disabled. Skipping trigger for workflow [{}] to subscriber [{}]", workflowId, subscriberId);
            return;
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "name", workflowId,
                    "to", Map.of("subscriberId", subscriberId),
                    "payload", payload != null ? payload : Map.of()
            );

            novuRestClient.post()
                    .uri("/events/trigger")
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully triggered Novu workflow [{}] for subscriber [{}]", workflowId, subscriberId);
        } catch (Exception e) {
            log.error("Failed to trigger Novu workflow [{}] for subscriber [{}]: {}", workflowId, subscriberId, e.getMessage());
        }
    }

    /**
     * Overload accepting UUID recipientId.
     */
    public void trigger(String workflowId, UUID recipientId, Map<String, Object> payload) {
        trigger(workflowId, recipientId.toString(), payload);
    }

    /**
     * Create or update a subscriber profile in Novu.
     *
     * @param subscriberId Unique ID of user
     * @param firstName    First name of user
     * @param lastName     Last name / username
     * @param email        Email of user
     * @param avatar       Profile picture URL (optional)
     */
    public void identifySubscriber(String subscriberId, String firstName, String lastName, String email, String avatar) {
        if (!properties.isEnabled()) {
            log.debug("Novu is disabled. Skipping identify for subscriber [{}]", subscriberId);
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("subscriberId", subscriberId);
            if (firstName != null) body.put("firstName", firstName);
            if (lastName != null) body.put("lastName", lastName);
            if (email != null) body.put("email", email);
            if (avatar != null) body.put("avatar", avatar);

            novuRestClient.post()
                    .uri("/subscribers")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Identified subscriber [{}] in Novu", subscriberId);
        } catch (Exception e) {
            log.error("Failed to identify subscriber [{}] in Novu: {}", subscriberId, e.getMessage());
        }
    }

    /**
     * Register a mobile / browser push device token for the subscriber.
     *
     * @param subscriberId User UUID string
     * @param providerId   "fcm" or "apns"
     * @param deviceToken  The raw push token from Firebase or APNs
     */
    public void setSubscriberCredentials(String subscriberId, String providerId, String deviceToken) {
        if (!properties.isEnabled()) {
            log.debug("Novu is disabled. Skipping credential registration for subscriber [{}]", subscriberId);
            return;
        }

        try {
            Map<String, Object> body = Map.of(
                    "providerId", providerId != null ? providerId : "fcm",
                    "deviceTokens", List.of(deviceToken)
            );

            novuRestClient.put()
                    .uri("/subscribers/{subscriberId}/credentials", subscriberId)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Registered [{}] device token for subscriber [{}] in Novu", providerId, subscriberId);
        } catch (Exception e) {
            log.error("Failed to register device token in Novu for subscriber [{}]: {}", subscriberId, e.getMessage());
        }
    }

    public void setSubscriberCredentials(UUID subscriberId, String providerId, String deviceToken) {
        setSubscriberCredentials(subscriberId.toString(), providerId, deviceToken);
    }
}

