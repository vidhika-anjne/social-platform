package com.vidhika.user_service.controller;

import com.vidhika.user_service.dto.CountResponse;
import com.vidhika.user_service.dto.FollowResponse;
import com.vidhika.user_service.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    // POST /api/v1/users/{targetUserId}/follow
    @PostMapping("/{targetUserId}/follow")
    public ResponseEntity<FollowResponse> follow(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID targetUserId) {

        UUID followerId = UUID.fromString(jwt.getSubject());
        FollowResponse response = followService.follow(followerId, targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // DELETE /api/v1/users/{targetUserId}/follow
    @DeleteMapping("/{targetUserId}/follow")
    public ResponseEntity<Void> unfollow(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID targetUserId) {

        UUID followerId = UUID.fromString(jwt.getSubject());
        followService.unfollow(followerId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/users/{userId}/followers
    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<FollowResponse>> getFollowers(@PathVariable UUID userId) {
        return ResponseEntity.ok(followService.getFollowers(userId));
    }

    // GET /api/v1/users/{userId}/following
    @GetMapping("/{userId}/following")
    public ResponseEntity<List<FollowResponse>> getFollowing(@PathVariable UUID userId) {
        return ResponseEntity.ok(followService.getFollowing(userId));
    }

    // GET /api/v1/users/{userId}/followers/count
    @GetMapping("/{userId}/followers/count")
    public ResponseEntity<CountResponse> getFollowersCount(@PathVariable UUID userId) {
        return ResponseEntity.ok(followService.getFollowersCount(userId));
    }

    // GET /api/v1/users/{userId}/following/count
    @GetMapping("/{userId}/following/count")
    public ResponseEntity<CountResponse> getFollowingCount(@PathVariable UUID userId) {
        return ResponseEntity.ok(followService.getFollowingCount(userId));
    }
}
