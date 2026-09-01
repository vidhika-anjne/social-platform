package com.vidhika.user_service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FollowResponse {

    private UUID followerId;
    private UUID followingId;
    private LocalDateTime createdAt;
}
