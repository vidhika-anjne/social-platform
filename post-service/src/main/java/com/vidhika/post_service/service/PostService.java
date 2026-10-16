package com.vidhika.post_service.service;

import com.vidhika.post_service.dto.CreatePostRequest;
import com.vidhika.post_service.dto.PostResponse;
import com.vidhika.post_service.dto.UpdatePostRequest;
import com.vidhika.post_service.enums.OutboxStatus;
import com.vidhika.post_service.exception.FailedUploadException;
import com.vidhika.post_service.exception.PostNotFoundException;
import com.vidhika.post_service.exception.UnauthorizedActionException;
import com.vidhika.post_service.model.OutboxEvent;
import com.vidhika.post_service.model.Post;
import com.vidhika.post_service.repository.OutboxEventRepository;
import com.vidhika.post_service.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final FileService fileService;

    @Transactional
    public PostResponse createPost(UUID userId, CreatePostRequest request) {
        // 1. Save the Post
        List<String> mediaUrls;
        try {
            mediaUrls = fileService.uploadMediaFiles(request.getFiles());
            log.info("Successfully uploaded the files to S3, with userId = {}",userId);
        } catch (IOException e) {
            log.error("Failed to upload the files, with userId = {}",userId);
            throw new FailedUploadException("Failed to upload the file with userId = "+userId);
        }

        Post post = Post.builder()
                .userId(userId)
                .content(request.getContent())
                .mediaUrls(mediaUrls)
                .build();

        Post saved = postRepository.save(post);
        log.info("Post created with id: {} for userId: {}", saved.getId(), userId);

        // 2. Save Outbox Event (POST_CREATED) in the same transaction
        UUID eventId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(eventId)
                .aggregateId(saved.getId())
                .eventType("POST_CREATED")
                .payload("""
                        {
                          "eventId": "%s",
                          "eventType": "POST_CREATED",
                          "postId": "%s",
                          "authorId": "%s",
                          "createdAt": "%s"
                        }
                        """.formatted(
                                eventId,
                                saved.getId(),
                                userId,
                                createdAt
                        ))
                .createdAt(createdAt)
                .processed(false)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxEventRepository.save(outboxEvent);
        log.info("OutboxEvent saved successfully with eventId = {}",eventId);
        return mapToResponse(saved);
    }
    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId.toString()));
        return mapToResponse(post);
    }

    public List<PostResponse> getMyPosts(UUID userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    @Transactional
    public PostResponse updatePost(UUID postId, UUID userId, UpdatePostRequest request) throws IOException {
        log.info("POST_UPDATE_REQUEST | userId = {} | postId = {}",userId,postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> {
                    log.error("POST_NOT_FOUND | postId = {}",postId);
                    return new PostNotFoundException(postId.toString());
                });

        if (!post.getUserId().equals(userId)) {
            log.error("USER_NOT_AUTHORIZED | userId = {} | postId = {}",userId,postId);
            throw new UnauthorizedActionException("You are not allowed to update this post");
        }

        if (request.getContent() != null && !request.getContent().isBlank()) {
            post.setContent(request.getContent());
        }
        if (request.getFiles() != null) {

            post.setMediaUrls(fileService.uploadMediaFiles(request.getFiles()));
        }

        Post updated = postRepository.save(post);
        log.info("POST_UPDATE_SUCCESS | userId = {} | postId = {}",userId,postId);
        return mapToResponse(updated);
    }

    public void deletePost(UUID postId, UUID userId) {
        log.info("POST_DELETE_REQUEST | userId = {} | postId = {}",userId,postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                {
                    log.error("POST_NOT_FOUND | postId = {}",postId);
                    return new PostNotFoundException(postId.toString());
                });

        if (!post.getUserId().equals(userId)) {
            log.error("USER_NOT_AUTHORIZED | userId = {} | postId = {}",userId,postId);
            throw new UnauthorizedActionException("You are not allowed to delete this post");
        }
        // Delete file from S3 make this process Async.
        postRepository.delete(post);
        log.info("POST_DELETE_SUCCESS | userId = {} | postId = {}",userId,postId);
    }

    private PostResponse mapToResponse(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .content(post.getContent())
                .mediaUrls(post.getMediaUrls())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
