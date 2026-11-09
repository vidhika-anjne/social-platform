package com.vidhika.chat_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service to manage real-time user presence and active room tracking using Redis.
 * Keys use TTLs so stale sessions automatically expire if a client drops ungracefully.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redisTemplate;

    private static final String ONLINE_PREFIX = "presence:user:%s:online";
    private static final String ACTIVE_CHAT_PREFIX = "presence:user:%s:active_chat";
    private static final Duration ONLINE_TTL = Duration.ofSeconds(60);
    private static final Duration ACTIVE_CHAT_TTL = Duration.ofSeconds(120);

    /**
     * Mark user as online in the app with an expiring TTL.
     */
    public void setUserOnline(String userId) {
        if (userId == null) return;
        try {
            String key = String.format(ONLINE_PREFIX, userId);
            redisTemplate.opsForValue().set(key, "1", ONLINE_TTL);
            log.debug("User {} marked online in Redis", userId);
        } catch (Exception e) {
            log.warn("Failed to set user {} online in Redis: {}", userId, e.getMessage());
        }
    }

    /**
     * Mark user as offline (e.g. on WebSocket disconnect).
     */
    public void setUserOffline(String userId) {
        if (userId == null) return;
        try {
            redisTemplate.delete(String.format(ONLINE_PREFIX, userId));
            redisTemplate.delete(String.format(ACTIVE_CHAT_PREFIX, userId));
            log.debug("User {} marked offline in Redis", userId);
        } catch (Exception e) {
            log.warn("Failed to set user {} offline in Redis: {}", userId, e.getMessage());
        }
    }

    /**
     * Check if user is currently online in the app.
     */
    public boolean isUserOnline(String userId) {
        if (userId == null) return false;
        try {
            Boolean hasKey = redisTemplate.hasKey(String.format(ONLINE_PREFIX, userId));
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.warn("Failed to check online status for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Record which conversation the user is currently viewing/chatting in.
     */
    public void setActiveChatRoom(String userId, String conversationId) {
        if (userId == null || conversationId == null) return;
        try {
            String key = String.format(ACTIVE_CHAT_PREFIX, userId);
            redisTemplate.opsForValue().set(key, conversationId, ACTIVE_CHAT_TTL);
            log.debug("User {} active chat room set to {}", userId, conversationId);
        } catch (Exception e) {
            log.warn("Failed to set active chat room for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Clear the user's active chat room (e.g. when leaving the chat window).
     */
    public void clearActiveChatRoom(String userId) {
        if (userId == null) return;
        try {
            redisTemplate.delete(String.format(ACTIVE_CHAT_PREFIX, userId));
            log.debug("Cleared active chat room for user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to clear active chat room for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Returns true if the user currently has this exact conversation room open.
     */
    public boolean isUserInRoom(String userId, String conversationId) {
        if (userId == null || conversationId == null) return false;
        try {
            String activeRoom = redisTemplate.opsForValue().get(String.format(ACTIVE_CHAT_PREFIX, userId));
            return conversationId.equals(activeRoom);
        } catch (Exception e) {
            log.warn("Failed to check active chat room for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
}

