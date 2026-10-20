package com.vidhika.user_service.controller;

import com.vidhika.user_service.model.UserProfile;
import com.vidhika.user_service.repository.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class SuggestionController
{
    private final SuggestionService suggestionService;
    // GET /api/v1/users/suggestions
    @GetMapping("/suggestions")
    public ResponseEntity<List<UserProfile>> getSuggestion(@AuthenticationPrincipal Jwt jwt)
    {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(suggestionService.getSuggestions(userId));
    }
}
