package com.vidhika.post_service.dto;

import com.vidhika.post_service.enums.RegistrationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRegistrationStatusRequest {

    @NotNull(message = "Status is required")
    private RegistrationStatus status;
}
