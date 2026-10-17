package com.vidhika.post_service.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.post_service.model.Post;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Data
@Builder
public class PostCleanupEvent
{
    private UUID eventId;
    private UUID aggregateId;
    private UUID postId;
    private String eventType;
    private List<String> mediaKeys;
    private LocalDateTime createdAt;
    public static String buildPayload(Post post, UUID eventId, LocalDateTime createdAt) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        PostCleanupEvent postMediaCleanupEvent = PostCleanupEvent.builder()
                .eventId(eventId)
                .eventType("POST_CLEANUP")
                .mediaKeys(post.getMediaUrls())
                .aggregateId(post.getId())
                .postId(post.getId())
                .createdAt(createdAt)
                .build();
        return objectMapper.writeValueAsString(postMediaCleanupEvent);
    }
}
// Specific to Post cleanup outbox request