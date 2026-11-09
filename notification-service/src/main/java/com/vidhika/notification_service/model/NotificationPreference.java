package com.vidhika.notification_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
    name = "notification_preferences",
    indexes = {
        @Index(name = "idx_notif_pref_user_id", columnList = "user_id", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Builder.Default
    @Column(name = "dnd_enabled", nullable = false)
    private boolean dndEnabled = false;

    /**
     * Start time for DND (e.g., 22:00 for 10 PM)
     */
    @Column(name = "dnd_start_time")
    private LocalTime dndStartTime;

    /**
     * End time for DND (e.g., 07:00 for 7 AM)
     */
    @Column(name = "dnd_end_time")
    private LocalTime dndEndTime;

    @Builder.Default
    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    @Builder.Default
    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Builder.Default
    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Checks if DND is currently active based on the provided time.
     * Handles intervals that cross midnight (e.g. 22:00 -> 07:00).
     */
    public boolean isDndActiveNow(LocalTime now) {
        if (!dndEnabled || dndStartTime == null || dndEndTime == null) {
            return false;
        }

        if (dndStartTime.isBefore(dndEndTime)) {
            // Same day range (e.g. 13:00 to 15:00)
            return !now.isBefore(dndStartTime) && !now.isAfter(dndEndTime);
        } else {
            // Overnight range spanning midnight (e.g. 22:00 to 07:00)
            return !now.isBefore(dndStartTime) || !now.isAfter(dndEndTime);
        }
    }
}
