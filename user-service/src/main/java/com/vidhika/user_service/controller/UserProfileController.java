package com.vidhika.user_service.controller;

import com.vidhika.user_service.dto.CreateProfileRequest;
import com.vidhika.user_service.dto.ProfileResponse;
import com.vidhika.user_service.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping("/create-profile")
    public ResponseEntity<ProfileResponse> createProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProfileRequest request) {

        // Extract UUID from JWT sub claim
        String sub = jwt.getSubject();
        UUID userId = UUID.fromString(sub);

        // Extract email claim from JWT if available
        String email = jwt.getClaimAsString("email");

        ProfileResponse response = userProfileService.createProfile(userId, email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
