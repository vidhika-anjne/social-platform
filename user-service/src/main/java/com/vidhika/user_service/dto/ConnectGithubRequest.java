package com.vidhika.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectGithubRequest {

    @NotBlank(message = "GitHub username is required")
    private String githubUsername;

    private String githubAccessToken;
}
