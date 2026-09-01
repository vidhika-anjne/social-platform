package com.vidhika.user_service.dto;

import com.vidhika.user_service.enums.Gender;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private UUID id;
    private String email;
    private String name;
    private String number;
    private Integer age;
    private Gender gender;
    private String college;
    private LocalDateTime createdAt;
    private String educationDegree;
    private String currentYear;
    private String city;
    private String bio;
    private String pic;
}
