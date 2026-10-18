package com.vidhika.post_service.dto;

import com.vidhika.post_service.enums.HackathonMode;
import com.vidhika.post_service.enums.HackathonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHackathonRequest {

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
}
