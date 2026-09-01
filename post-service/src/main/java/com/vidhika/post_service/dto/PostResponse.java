package com.vidhika.post_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PostResponse {

    private UUID id;
    private UUID userId;
    private String content;
    private String mediaUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
