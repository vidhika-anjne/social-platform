package com.vidhika.post_service.controller;

import com.vidhika.post_service.dto.CommentResponse;
import com.vidhika.post_service.dto.CreateCommentRequest;
import com.vidhika.post_service.dto.UpdateCommentRequest;
import com.vidhika.post_service.service.CommentService;
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
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // POST /api/v1/posts/{postId}/comments
    @PostMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        CommentResponse response = commentService.createComment(postId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/posts/{postId}/comments
    @GetMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable UUID postId) {
        return ResponseEntity.ok(commentService.getComments(postId));
    }

    // PUT /api/v1/comments/{commentId}
    @PutMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        CommentResponse response = commentService.updateComment(commentId, userId, request);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/v1/comments/{commentId}
    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID commentId) {

        UUID userId = UUID.fromString(jwt.getSubject());
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
