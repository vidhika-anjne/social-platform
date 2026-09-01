package com.vidhika.user_service.service;

import com.vidhika.user_service.dto.CreateProfileRequest;
import com.vidhika.user_service.dto.ProfileResponse;
import com.vidhika.user_service.model.UserProfile;
import com.vidhika.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public ProfileResponse createProfile(UUID userId, String email, CreateProfileRequest request) {
        if (userProfileRepository.existsById(userId)) {
            throw new RuntimeException("Profile already exists for user ID: " + userId);
        }

        String finalEmail = (email != null && !email.isBlank()) ? email : request.getEmail();

        UserProfile profile = UserProfile.builder()
                .id(userId)
                .email(finalEmail)
                .name(request.getName())
                .number(request.getNumber())
                .age(request.getAge())
                .gender(request.getGender())
                .college(request.getCollege())
                .educationDegree(request.getEducationDegree())
                .currentYear(request.getCurrentYear())
                .city(request.getCity())
                .bio(request.getBio())
                .pic(request.getPic())
                .build();

        UserProfile saved = userProfileRepository.save(profile);
        return mapToResponse(saved);
    }

    private ProfileResponse mapToResponse(UserProfile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .email(profile.getEmail())
                .name(profile.getName())
                .number(profile.getNumber())
                .age(profile.getAge())
                .gender(profile.getGender())
                .college(profile.getCollege())
                .createdAt(profile.getCreatedAt())
                .educationDegree(profile.getEducationDegree())
                .currentYear(profile.getCurrentYear())
                .city(profile.getCity())
                .bio(profile.getBio())
                .pic(profile.getPic())
                .build();
    }
}
