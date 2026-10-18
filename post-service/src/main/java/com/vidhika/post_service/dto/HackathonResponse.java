package com.vidhika.post_service.dto;

import com.vidhika.post_service.enums.HackathonMode;
import com.vidhika.post_service.enums.HackathonStatus;
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
public class HackathonResponse {

    private UUID id;
    private UUID organizerId;
    private String title;
    private String description;
    private String bannerUrl;
    private String logoUrl;
    private HackathonMode mode;
    private String venue;
    private String websiteUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime registrationDeadline;
    private Integer minTeamSize;
    private Integer maxTeamSize;
    private HackathonStatus status;
    private Long registrationCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
