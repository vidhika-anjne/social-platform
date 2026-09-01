package com.vidhika.user_service.service;

import com.vidhika.user_service.dto.CreateProfileRequest;
import com.vidhika.user_service.dto.ProfileResponse;
import com.vidhika.user_service.model.UserProfile;
import com.vidhika.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public void createUserProfileFromEvent(UUID userId, String email, String name) {
        if (userProfileRepository.existsById(userId)) {
            log.info("User profile already exists for userId: {}", userId);
            return;
        }

        UserProfile profile = UserProfile.builder()
                .id(userId)
                .email(email)
                .name(name != null && !name.isBlank() ? name : "User")
                .build();

        userProfileRepository.save(profile);
        log.info("Created user profile entry via Kafka event for userId: {}", userId);
    }

    public ProfileResponse createProfile(UUID userId, String email, CreateProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> UserProfile.builder().id(userId).build());

        String finalEmail = (email != null && !email.isBlank()) ? email : request.getEmail();
        if (finalEmail != null && !finalEmail.isBlank()) {
            profile.setEmail(finalEmail);
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            profile.setName(request.getName());
        } else if (profile.getName() == null) {
            profile.setName("User");
        }

        if (request.getNumber() != null) profile.setNumber(request.getNumber());
        if (request.getAge() != null) profile.setAge(request.getAge());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getCollege() != null) profile.setCollege(request.getCollege());
        if (request.getEducationDegree() != null) profile.setEducationDegree(request.getEducationDegree());
        if (request.getCurrentYear() != null) profile.setCurrentYear(request.getCurrentYear());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getPic() != null) profile.setPic(request.getPic());

        UserProfile saved = userProfileRepository.save(profile);
        return mapToResponse(saved);
    }

    public ProfileResponse getProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for userId: " + userId));
        return mapToResponse(profile);
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
