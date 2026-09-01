package com.vidhika.user_service.model;

import com.vidhika.user_service.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private UUID id; // Primary key matches auth-service User UUID (from JWT sub)

    private String email;

    @Column(nullable = false)
    private String name;

    private String number;

    private Integer age;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String college;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private String educationDegree;

    private String currentYear;

    private String city;

    @Column(length = 500)
    private String bio;

    @Column(length = 1000)
    private String pic;
}
