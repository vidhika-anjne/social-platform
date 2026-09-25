package com.vidhika.feed_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedItemResponse {
    private UUID id;
    private UUID userId;
    private UUID postId;
    private LocalDateTime createdAt;
}
