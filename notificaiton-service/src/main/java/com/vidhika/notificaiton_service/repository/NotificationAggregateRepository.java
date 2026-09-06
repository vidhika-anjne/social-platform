package com.vidhika.notificaiton_service.repository;

import com.vidhika.notificaiton_service.model.NotificationAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationAggregateRepository extends JpaRepository<NotificationAggregate, UUID> {

    /**
     * Find the active (not-yet-notified) aggregation window for a given
     * recipientId + postId + notificationType whose window has not ended yet.
     */
    Optional<NotificationAggregate> findByRecipientIdAndPostIdAndNotificationTypeAndNotifiedFalseAndWindowEndAfter(
            UUID recipientId,
            UUID postId,
            String notificationType,
            LocalDateTime now
    );

    /**
     * Find all completed aggregation windows that haven't been flushed yet.
     * windowEnd <= now AND notified = false
     */
    List<NotificationAggregate> findByNotifiedFalseAndWindowEndBefore(LocalDateTime now);
}
