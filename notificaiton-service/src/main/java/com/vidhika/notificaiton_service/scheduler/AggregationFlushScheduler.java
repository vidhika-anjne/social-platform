package com.vidhika.notificaiton_service.scheduler;

import com.vidhika.notificaiton_service.model.Notification;
import com.vidhika.notificaiton_service.model.NotificationAggregate;
import com.vidhika.notificaiton_service.repository.NotificationAggregateRepository;
import com.vidhika.notificaiton_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that periodically finds completed aggregation windows
 * and flushes them into actual Notification rows.
 * Runs every 30 seconds.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationFlushScheduler {

    private final NotificationAggregateRepository aggregateRepository;
    private final NotificationRepository notificationRepository;

    @Scheduled(fixedRate = 30000) // every 30 seconds
    @Transactional
    public void flushCompletedAggregations() {
        LocalDateTime now = LocalDateTime.now();

        List<NotificationAggregate> completedWindows =
                aggregateRepository.findByNotifiedFalseAndWindowEndBefore(now);

        if (completedWindows.isEmpty()) {
            return;
        }

        log.info("Flushing {} completed aggregation windows into notifications...", completedWindows.size());

        for (NotificationAggregate aggregate : completedWindows) {
            try {
                // Build aggregated notification message
                String message = buildMessage(aggregate);

                Notification notification = Notification.builder()
                        .recipientId(aggregate.getRecipientId())
                        .type(aggregate.getNotificationType())
                        .message(message)
                        .postId(aggregate.getPostId())
                        .actorId(aggregate.getLastActorId())
                        .read(false)
                        .build();

                notificationRepository.save(notification);

                // Mark aggregation as notified
                aggregate.setNotified(true);
                aggregateRepository.save(aggregate);

                log.info("Created notification for recipientId={}, postId={}, likeCount={}",
                        aggregate.getRecipientId(), aggregate.getPostId(), aggregate.getCount());

            } catch (Exception e) {
                log.error("Failed to flush aggregation {}: {}", aggregate.getId(), e.getMessage(), e);
            }
        }
    }

    private String buildMessage(NotificationAggregate aggregate) {
        int count = aggregate.getCount();
        if (count == 1) {
            return "Someone liked your post.";
        } else {
            return count + " people liked your post.";
        }
    }
}
