package com.vidhika.notificaiton_service.service;

import com.vidhika.notificaiton_service.model.NotificationAggregate;
import com.vidhika.notificaiton_service.repository.NotificationAggregateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Responsible for managing aggregation windows.
 * Kafka consumer calls this to record each incoming like event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAggregationService {

    private final NotificationAggregateRepository aggregateRepository;

    @Value("${notification.aggregation.window-minutes:5}")
    private int windowMinutes;

    /**
     * Record a POST_LIKED event into an aggregation window.
     * If an active window exists → increment count.
     * If not → create a new window with count = 1.
     */
    @Transactional
    public void recordLikeEvent(UUID recipientId, UUID postId, UUID actorId) {
        LocalDateTime now = LocalDateTime.now();
        String notificationType = "POST_LIKED";

        Optional<NotificationAggregate> existing = aggregateRepository
                .findByRecipientIdAndPostIdAndNotificationTypeAndNotifiedFalseAndWindowEndAfter(
                        recipientId, postId, notificationType, now
                );

        if (existing.isPresent()) {
            // Active window found → increment count and update last actor
            NotificationAggregate aggregate = existing.get();
            aggregate.setCount(aggregate.getCount() + 1);
            aggregate.setLastActorId(actorId);
            aggregateRepository.save(aggregate);
            log.info("Incremented aggregation {} to count={} for recipientId={}, postId={}",
                    aggregate.getId(), aggregate.getCount(), recipientId, postId);
        } else {
            // No active window → create new one
            NotificationAggregate aggregate = NotificationAggregate.builder()
                    .recipientId(recipientId)
                    .postId(postId)
                    .notificationType(notificationType)
                    .lastActorId(actorId)
                    .count(1)
                    .windowStart(now)
                    .windowEnd(now.plusMinutes(windowMinutes))
                    .notified(false)
                    .build();
            aggregateRepository.save(aggregate);
            log.info("Created new aggregation window for recipientId={}, postId={}, windowEnd={}",
                    recipientId, postId, aggregate.getWindowEnd());
        }
    }
}
