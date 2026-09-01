package com.vidhika.post_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LikeResponse {

    private UUID id;
    private UUID postId;
    private UUID userId;
    private LocalDateTime createdAt;
}
