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
public class SelectRepositoryRequest {

    @NotBlank(message = "Repository owner is required")
    private String owner;

    @NotBlank(message = "Repository name is required")
    private String repo;
}
