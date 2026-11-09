package com.vidhika.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Anti-flood throttling service.
 * Prevents blasting multiple push notifications to a user when another user rapidly types multiple messages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationThrottlingService {

    private final StringRedisTemplate redisTemplate;

    private static final String THROTTLE_KEY = "chat:throttle:%s:%s";
    private static final Duration THROTTLE_TTL = Duration.ofSeconds(5);

    /**
     * Checks if notification should be throttled.
     * Uses atomic Redis setIfAbsent: if key exists within the 5s window, returns true (throttled).
     *
     * @param recipientId    User receiving the alert
     * @param conversationId The chat room
     * @return true if throttled (skip push), false if allowed.
     */
    public boolean shouldThrottle(String recipientId, String conversationId) {
        if (redisTemplate == null) return false;
        try {
            String key = String.format(THROTTLE_KEY, recipientId, conversationId);
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", THROTTLE_TTL);
            return Boolean.FALSE.equals(isNew);
        } catch (Exception e) {
            log.warn("Redis throttling check failed: {}", e.getMessage());
            return false;
        }
    }
}
