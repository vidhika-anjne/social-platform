package com.vidhika.notification_service.dto.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserFollowedEvent {

    private String eventId;
    private String eventType;
    private UUID followerId;
    private String followerName;
    private String followerAvatar;
    private UUID targetUserId;
    private LocalDateTime createdAt;
}

