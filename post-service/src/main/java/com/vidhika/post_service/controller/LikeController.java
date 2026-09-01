package com.vidhika.post_service.controller;

import com.vidhika.post_service.dto.LikeResponse;
import com.vidhika.post_service.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    // POST /api/v1/posts/{postId}/like
    @PostMapping("/{postId}/like")
    public ResponseEntity<LikeResponse> likePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        LikeResponse response = likeService.likePost(postId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // DELETE /api/v1/posts/{postId}/like
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<Void> unlikePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        likeService.unlikePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/posts/{postId}/likes
    @GetMapping("/{postId}/likes")
    public ResponseEntity<List<LikeResponse>> getLikes(@PathVariable UUID postId) {
        return ResponseEntity.ok(likeService.getLikes(postId));
    }
}
