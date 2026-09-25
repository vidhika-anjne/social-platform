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
public class FollowResponse {
    private UUID followerId;
    private UUID followingId;
    private LocalDateTime createdAt;
}
