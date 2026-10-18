package com.vidhika.post_service.controller;

import com.vidhika.post_service.dto.*;
import com.vidhika.post_service.enums.HackathonStatus;
import com.vidhika.post_service.service.HackathonRegistrationService;
import com.vidhika.post_service.service.HackathonService;
import com.vidhika.post_service.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hackathons")
@RequiredArgsConstructor
public class HackathonController {

    private final HackathonService hackathonService;
    private final HackathonRegistrationService registrationService;
    private final PostService postService;

    // POST /api/v1/hackathons (Organizer creates hackathon)
    @PostMapping
    public ResponseEntity<HackathonResponse> createHackathon(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateHackathonRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String role = jwt.getClaimAsString("role");
        HackathonResponse response = hackathonService.createHackathon(userId, role, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/hackathons/my (Organizer's own hackathons)
    @GetMapping("/my")
    public ResponseEntity<List<HackathonResponse>> getMyHackathons(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<HackathonResponse> responses = hackathonService.getMyHackathons(userId);
        return ResponseEntity.ok(responses);
    }

    // GET /api/v1/hackathons/my-registrations (User's hackathon registrations)
    @GetMapping("/my-registrations")
    public ResponseEntity<List<HackathonRegistrationResponse>> getMyRegistrations(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<HackathonRegistrationResponse> responses = registrationService.getMyRegistrations(userId);
        return ResponseEntity.ok(responses);
    }

    // GET /api/v1/hackathons (Browse all hackathons or filter by status)
    @GetMapping
    public ResponseEntity<List<HackathonResponse>> getAllHackathons(
            @RequestParam(required = false) HackathonStatus status) {

        List<HackathonResponse> responses = hackathonService.getAllHackathons(status);
        return ResponseEntity.ok(responses);
    }

    // GET /api/v1/hackathons/{id} (Hackathon details)
    @GetMapping("/{id}")
    public ResponseEntity<HackathonResponse> getHackathon(@PathVariable UUID id) {
        HackathonResponse response = hackathonService.getHackathon(id);
        return ResponseEntity.ok(response);
    }

    // PUT /api/v1/hackathons/{id} (Organizer updates hackathon)
    @PutMapping("/{id}")
    public ResponseEntity<HackathonResponse> updateHackathon(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHackathonRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String role = jwt.getClaimAsString("role");
        HackathonResponse response = hackathonService.updateHackathon(id, userId, role, request);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/v1/hackathons/{id} (Organizer deletes hackathon)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHackathon(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String role = jwt.getClaimAsString("role");
        hackathonService.deleteHackathon(id, userId, role);
        return ResponseEntity.noContent().build();
    }

    // POST /api/v1/hackathons/{id}/register (User registers for hackathon)
    @PostMapping("/{id}/register")
    public ResponseEntity<HackathonRegistrationResponse> registerForHackathon(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody(required = false) RegisterHackathonRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        HackathonRegistrationResponse response = registrationService.registerForHackathon(id, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/hackathons/{id}/registrations (Organizer views registrations)
    @GetMapping("/{id}/registrations")
    public ResponseEntity<List<HackathonRegistrationResponse>> getRegistrations(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String role = jwt.getClaimAsString("role");
        List<HackathonRegistrationResponse> responses = registrationService.getRegistrationsForHackathon(id, userId, role);
        return ResponseEntity.ok(responses);
    }

    // PUT /api/v1/hackathons/{id}/registrations/{registrationId}/status (Organizer updates status: WINNER, SHORTLISTED, etc.)
    @PutMapping("/{id}/registrations/{registrationId}/status")
    public ResponseEntity<HackathonRegistrationResponse> updateRegistrationStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID registrationId,
            @Valid @RequestBody UpdateRegistrationStatusRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String role = jwt.getClaimAsString("role");
        HackathonRegistrationResponse response = registrationService.updateRegistrationStatus(id, registrationId, userId, role, request);
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/hackathons/{id}/teams/{teamName} (View all registered members of a team)
    @GetMapping("/{id}/teams/{teamName}")
    public ResponseEntity<List<HackathonRegistrationResponse>> getTeamMembers(
            @PathVariable UUID id,
            @PathVariable String teamName) {

        List<HackathonRegistrationResponse> responses = registrationService.getTeamMembers(id, teamName);
        return ResponseEntity.ok(responses);
    }

    // GET /api/v1/hackathons/{id}/posts (Showcase posts tagged with this hackathon)
    @GetMapping("/{id}/posts")
    public ResponseEntity<List<PostResponse>> getHackathonPosts(@PathVariable UUID id) {
        List<PostResponse> responses = postService.getPostsByHackathonId(id);
        return ResponseEntity.ok(responses);
    }
}
