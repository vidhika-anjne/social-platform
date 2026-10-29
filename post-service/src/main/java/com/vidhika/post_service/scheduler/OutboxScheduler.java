package com.vidhika.post_service.scheduler;

import com.vidhika.post_service.enums.OutboxStatus;
import com.vidhika.post_service.model.OutboxEvent;
import com.vidhika.post_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 5;
    private static final String DEFAULT_POST_TOPIC = "post-events";
    private static final String TEAM_EVENTS_TOPIC = "team-events";

    private String resolveTopic(OutboxEvent event) {
        if ("TEAM_FORMED".equalsIgnoreCase(event.getEventType())) {
            return TEAM_EVENTS_TOPIC;
        }
        return DEFAULT_POST_TOPIC;
    }

    @Scheduled(fixedRate = 5000) // Runs every 5 seconds
    public void processOutboxEvents() {
        List<OutboxEvent> eventsToProcess = outboxEventRepository.findByStatusInAndRetryCountLessThan(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                MAX_RETRIES,
                PageRequest.of(0, BATCH_SIZE)
        );

        if (eventsToProcess.isEmpty()) {
            return;
        }

        log.info("Processing {} outbox events in batch...", eventsToProcess.size());

        for (OutboxEvent event : eventsToProcess) {
            String targetTopic = resolveTopic(event);
            try {
                kafkaTemplate.send(targetTopic, event.getAggregateId().toString(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.info("Successfully published outbox event {} ({}) to Kafka topic {}",
                                        event.getId(), event.getEventType(), targetTopic);
                                event.setStatus(OutboxStatus.PUBLISHED);
                                event.setProcessed(true);
                            } else {
                                log.error("Failed to publish outbox event {} to Kafka topic {}: {}",
                                        event.getId(), targetTopic, ex.getMessage());
                                handleFailure(event);
                            }
                            outboxEventRepository.save(event);
                        });
            } catch (Exception e) {
                log.error("Exception while dispatching outbox event {} to topic {}: {}",
                        event.getId(), targetTopic, e.getMessage());
                handleFailure(event);
                outboxEventRepository.save(event);
            }
        }
    }
    
    private void handleFailure(OutboxEvent event) {
        int nextRetryCount = event.getRetryCount() + 1;
        event.setRetryCount(nextRetryCount);
        event.setStatus(OutboxStatus.FAILED);
        if (nextRetryCount >= MAX_RETRIES) {
            log.warn("Outbox event {} reached max retries ({}) and marked as FAILED.", event.getId(), MAX_RETRIES);
        }
    }
}