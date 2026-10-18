package com.vidhika.post_service.dto;

import com.vidhika.post_service.enums.HackathonMode;
import com.vidhika.post_service.enums.HackathonStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHackathonRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String bannerUrl;

    private String logoUrl;

    @NotNull(message = "Mode is required (ONLINE, IN_PERSON, HYBRID)")
    private HackathonMode mode;

    private String venue;

    private String websiteUrl;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotNull(message = "Registration deadline is required")
    private LocalDateTime registrationDeadline;

    @Min(value = 1, message = "Minimum team size must be at least 1")
    private Integer minTeamSize;

    @Min(value = 1, message = "Maximum team size must be at least 1")
    private Integer maxTeamSize;

    private HackathonStatus status;
}
