package com.vidhika.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRequest {

    @NotBlank(message = "Device token is required")
    private String token;

    @Builder.Default
    private String provider = "fcm"; // "fcm" (Firebase Cloud Messaging), "apns" (Apple)
}

