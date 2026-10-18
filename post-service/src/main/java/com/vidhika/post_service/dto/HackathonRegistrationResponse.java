package com.vidhika.post_service.dto;

import com.vidhika.post_service.enums.RegistrationStatus;
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
public class HackathonRegistrationResponse {

    private UUID id;
    private UUID hackathonId;
    private UUID userId;
    private String teamName;
    private RegistrationStatus status;
    private LocalDateTime registeredAt;
}
