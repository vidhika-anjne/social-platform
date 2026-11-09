package com.vidhika.notification_service.service;

import com.vidhika.notification_service.dto.NotificationPreferenceRequest;
import com.vidhika.notification_service.dto.NotificationPreferenceResponse;
import com.vidhika.notification_service.model.NotificationPreference;
import com.vidhika.notification_service.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceResponse getPreferences(UUID userId) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
        return mapToResponse(preference);
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(UUID userId, NotificationPreferenceRequest request) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));

        if (request.getDndEnabled() != null) {
            preference.setDndEnabled(request.getDndEnabled());
        }
        if (request.getDndStartTime() != null) {
            preference.setDndStartTime(request.getDndStartTime());
        }
        if (request.getDndEndTime() != null) {
            preference.setDndEndTime(request.getDndEndTime());
        }
        if (request.getPushEnabled() != null) {
            preference.setPushEnabled(request.getPushEnabled());
        }
        if (request.getInAppEnabled() != null) {
            preference.setInAppEnabled(request.getInAppEnabled());
        }
        if (request.getEmailEnabled() != null) {
            preference.setEmailEnabled(request.getEmailEnabled());
        }

        NotificationPreference saved = preferenceRepository.save(preference);
        log.info("Updated notification preferences for user {}", userId);
        return mapToResponse(saved);
    }

    private NotificationPreference createDefaultPreference(UUID userId) {
        NotificationPreference preference = NotificationPreference.builder()
                .userId(userId)
                .dndEnabled(false)
                .pushEnabled(true)
                .inAppEnabled(true)
                .emailEnabled(false)
                .build();
        return preferenceRepository.save(preference);
    }

    private NotificationPreferenceResponse mapToResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .dndEnabled(preference.isDndEnabled())
                .dndStartTime(preference.getDndStartTime())
                .dndEndTime(preference.getDndEndTime())
                .pushEnabled(preference.isPushEnabled())
                .inAppEnabled(preference.isInAppEnabled())
                .emailEnabled(preference.isEmailEnabled())
                .build();
    }
}

