package com.vidhika.post_service.service;

import com.vidhika.post_service.dto.LikeResponse;
import com.vidhika.post_service.enums.OutboxStatus;
import com.vidhika.post_service.exception.AlreadyLikedException;
import com.vidhika.post_service.exception.PostNotFoundException;
import com.vidhika.post_service.model.Like;
import com.vidhika.post_service.model.OutboxEvent;
import com.vidhika.post_service.model.Post;
import com.vidhika.post_service.repository.LikeRepository;
import com.vidhika.post_service.repository.OutboxEventRepository;
import com.vidhika.post_service.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public LikeResponse likePost(UUID postId, UUID userId) {
        // 1. Check post exists & retrieve post details
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId.toString()));

        // 2. Check if already liked
        if (likeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new AlreadyLikedException();
        }

        // 3. Save Like
        Like like = Like.builder()
                .postId(postId)
                .userId(userId)
                .build();

        Like saved = likeRepository.save(like);
        log.info("Post {} liked by user {}", postId, userId);

        // 4. Save Outbox Event (POST_LIKED)
        UUID eventId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(eventId)
                .aggregateId(postId)
                .eventType("POST_LIKED")
                .payload("""
                        {
                          "eventId": "%s",
                          "eventType": "POST_LIKED",
                          "aggregateId": "%s",
                          "postId": "%s",
                          "likerId": "%s",
                          "postOwnerId": "%s",
                          "createdAt": "%s"
                        }
                        """.formatted(
                                eventId,
                                postId,
                                postId,
                                userId,
                                post.getUserId(),
                                createdAt
                        ))
                .createdAt(createdAt)
                .processed(false)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxEventRepository.save(outboxEvent);

        return mapToResponse(saved);
    }

    @Transactional
    public void unlikePost(UUID postId, UUID userId) {
        Like like = likeRepository.findByPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new RuntimeException("You have not liked this post"));

        likeRepository.delete(like);
        log.info("Post {} unliked by user {}", postId, userId);
    }

    public List<LikeResponse> getLikes(UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId.toString());
        }

        return likeRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private LikeResponse mapToResponse(Like like) {
        return LikeResponse.builder()
                .id(like.getId())
                .postId(like.getPostId())
                .userId(like.getUserId())
                .createdAt(like.getCreatedAt())
                .build();
    }
}
