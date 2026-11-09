package com.vidhika.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private UUID id;
    private UUID userId;
    private boolean dndEnabled;
    private LocalTime dndStartTime;
    private LocalTime dndEndTime;
    private boolean pushEnabled;
    private boolean inAppEnabled;
    private boolean emailEnabled;
}

