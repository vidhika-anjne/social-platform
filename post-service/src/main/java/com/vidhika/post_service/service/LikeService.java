package com.vidhika.post_service.service;

import com.vidhika.post_service.dto.LikeResponse;
import com.vidhika.post_service.exception.AlreadyLikedException;
import com.vidhika.post_service.exception.PostNotFoundException;
import com.vidhika.post_service.model.Like;
import com.vidhika.post_service.repository.LikeRepository;
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
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    public LikeResponse likePost(UUID postId, UUID userId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId.toString());
        }

        if (likeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new AlreadyLikedException();
        }

        Like like = Like.builder()
                .postId(postId)
                .userId(userId)
                .build();

        Like saved = likeRepository.save(like);
        log.info("Post {} liked by user {}", postId, userId);
        return mapToResponse(saved);
    }

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
