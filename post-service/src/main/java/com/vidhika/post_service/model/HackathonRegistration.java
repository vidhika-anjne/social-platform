package com.vidhika.post_service.model;

import com.vidhika.post_service.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "hackathon_registrations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"hackathon_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HackathonRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hackathon_id", nullable = false)
    private UUID hackathonId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private String teamName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RegistrationStatus status = RegistrationStatus.REGISTERED;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime registeredAt;
}
