package com.vidhika.user_service.dto;

import com.vidhika.user_service.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String email;

    private String number;

    private Integer age;

    private Gender gender;

    private String college;

    private String educationDegree;

    private String currentYear;

    private String city;

    private String bio;

    private String pic;
}
