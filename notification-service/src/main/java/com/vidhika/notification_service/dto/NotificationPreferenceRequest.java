package com.vidhika.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequest {

    private Boolean dndEnabled;
    private LocalTime dndStartTime;
    private LocalTime dndEndTime;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;
    private Boolean emailEnabled;
}

