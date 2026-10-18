package com.vidhika.post_service.model;

import com.vidhika.post_service.enums.HackathonMode;
import com.vidhika.post_service.enums.HackathonStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hackathons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hackathon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID organizerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String bannerUrl;

    @Column(length = 1000)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HackathonMode mode;

    private String venue;

    private String websiteUrl;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private LocalDateTime registrationDeadline;

    @Builder.Default
    private Integer minTeamSize = 1;

    @Builder.Default
    private Integer maxTeamSize = 4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private HackathonStatus status = HackathonStatus.REGISTRATION_OPEN;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
