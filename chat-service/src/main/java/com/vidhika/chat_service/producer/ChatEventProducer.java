package com.vidhika.chat_service.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.chat_service.dto.kafka.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.chat-events:chat-events}")
    private String chatEventsTopic;

    public void publishMessageSentEvent(ChatMessageEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(chatEventsTopic, event.getConversationId(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish chat event to topic {} for conversation {}: {}",
                                    chatEventsTopic, event.getConversationId(), ex.getMessage());
                        } else {
                            log.info("Published CHAT_MESSAGE_SENT event for message {} in conversation {}",
                                    event.getMessageId(), event.getConversationId());
                        }
                    });
        } catch (Exception e) {
            log.error("Error serializing chat message event for conversation {}: {}",
                    event.getConversationId(), e.getMessage(), e);
        }
    }
}
