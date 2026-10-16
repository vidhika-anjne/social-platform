package com.vidhika.post_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PostResponse {

    private UUID id;
    private UUID userId;
    private String content;
    private List<String> mediaUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
