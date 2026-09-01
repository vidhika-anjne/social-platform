package com.vidhika.post_service.service;

import com.vidhika.post_service.dto.CommentResponse;
import com.vidhika.post_service.dto.CreateCommentRequest;
import com.vidhika.post_service.dto.UpdateCommentRequest;
import com.vidhika.post_service.exception.CommentNotFoundException;
import com.vidhika.post_service.exception.PostNotFoundException;
import com.vidhika.post_service.exception.UnauthorizedActionException;
import com.vidhika.post_service.model.Comment;
import com.vidhika.post_service.repository.CommentRepository;
import com.vidhika.post_service.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public CommentResponse createComment(UUID postId, UUID userId, CreateCommentRequest request) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId.toString());
        }

        Comment comment = Comment.builder()
                .postId(postId)
                .userId(userId)
                .content(request.getContent())
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("Comment created on post {} by user {}", postId, userId);
        return mapToResponse(saved);
    }

    public List<CommentResponse> getComments(UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId.toString());
        }

        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CommentResponse updateComment(UUID commentId, UUID userId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId.toString()));

        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You are not allowed to update this comment");
        }

        if (request.getContent() != null && !request.getContent().isBlank()) {
            comment.setContent(request.getContent());
        }

        Comment updated = commentRepository.save(comment);
        log.info("Comment {} updated by user {}", commentId, userId);
        return mapToResponse(updated);
    }

    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId.toString()));

        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedActionException("You are not allowed to delete this comment");
        }

        commentRepository.delete(comment);
        log.info("Comment {} deleted by user {}", commentId, userId);
    }

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
