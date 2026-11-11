package com.vidhika.user_service.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowedEvent {

    private String eventId;

    @Builder.Default
    private String eventType = "USER_FOLLOWED";

    private UUID followerId;
    private String followerName;
    private String followerAvatar;
    private UUID targetUserId;
    private LocalDateTime createdAt;
}
