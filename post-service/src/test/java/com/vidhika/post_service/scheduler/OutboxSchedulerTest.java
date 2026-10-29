package com.vidhika.post_service.scheduler;

import com.vidhika.post_service.enums.OutboxStatus;
import com.vidhika.post_service.model.OutboxEvent;
import com.vidhika.post_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxScheduler outboxScheduler;

    @BeforeEach
    void setUp() {
    }

    @Test
    void processOutboxEvents_TeamFormedEvent_SentToTeamEventsTopic() {
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent teamEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(aggregateId)
                .eventType("TEAM_FORMED")
                .payload("{\"eventType\":\"TEAM_FORMED\",\"teamId\":\"" + aggregateId + "\"}")
                .createdAt(LocalDateTime.now())
                .processed(false)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(outboxEventRepository.findByStatusInAndRetryCountLessThan(anyList(), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(teamEvent));

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("team-events"), eq(aggregateId.toString()), anyString()))
                .thenReturn(future);

        outboxScheduler.processOutboxEvents();

        verify(kafkaTemplate, times(1)).send(eq("team-events"), eq(aggregateId.toString()), eq(teamEvent.getPayload()));
        verify(kafkaTemplate, never()).send(eq("post-events"), any(), any());
        verify(outboxEventRepository, times(1)).save(teamEvent);
        assertThat(teamEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(teamEvent.isProcessed()).isTrue();
    }

    @Test
    void processOutboxEvents_PostEvent_SentToPostEventsTopic() {
        UUID aggregateId = UUID.randomUUID();
        OutboxEvent postEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(aggregateId)
                .eventType("POST_CLEANUP")
                .payload("{\"postId\":\"" + aggregateId + "\"}")
                .createdAt(LocalDateTime.now())
                .processed(false)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        when(outboxEventRepository.findByStatusInAndRetryCountLessThan(anyList(), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(postEvent));

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("post-events"), eq(aggregateId.toString()), anyString()))
                .thenReturn(future);

        outboxScheduler.processOutboxEvents();

        verify(kafkaTemplate, times(1)).send(eq("post-events"), eq(aggregateId.toString()), eq(postEvent.getPayload()));
        verify(kafkaTemplate, never()).send(eq("team-events"), any(), any());
        verify(outboxEventRepository, times(1)).save(postEvent);
        assertThat(postEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(postEvent.isProcessed()).isTrue();
    }
}
