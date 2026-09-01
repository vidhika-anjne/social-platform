package com.vidhika.post_service.controller;

import com.vidhika.post_service.dto.CreatePostRequest;
import com.vidhika.post_service.dto.PostResponse;
import com.vidhika.post_service.dto.UpdatePostRequest;
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
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // POST /api/v1/posts
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePostRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        PostResponse response = postService.createPost(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/posts/{postId}
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable UUID postId) {
        PostResponse response = postService.getPost(postId);
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/posts/my
    @GetMapping("/my")
    public ResponseEntity<List<PostResponse>> getMyPosts(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<PostResponse> responses = postService.getMyPosts(userId);
        return ResponseEntity.ok(responses);
    }

    // PUT /api/v1/posts/{postId}
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        PostResponse response = postService.updatePost(postId, userId, request);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/v1/posts/{postId}
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        postService.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }
}
