package com.vidhika.feed_service.service;

import com.vidhika.feed_service.client.UserServiceClient;
import com.vidhika.feed_service.dto.FeedItemResponse;
import com.vidhika.feed_service.dto.FollowResponse;
import com.vidhika.feed_service.model.FeedItem;
import com.vidhika.feed_service.repository.FeedItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedItemRepository feedItemRepository;
    private final UserServiceClient userServiceClient;

    @Transactional
    public void processPostCreatedEvent(UUID authorId, UUID postId, LocalDateTime eventCreatedAt) {
        log.info("Processing POST_CREATED event for postId: {} by authorId: {}", postId, authorId);

        // Fetch followers of the post author from user-service using WebClient
        List<FollowResponse> followers = userServiceClient.getFollowers(authorId);
        log.info("Fetched {} followers for authorId: {}", followers.size(), authorId);

        List<FeedItem> feedItemsToSave = new ArrayList<>();
        for (FollowResponse follower : followers) {
            FeedItem item = FeedItem.builder()
                    .userId(follower.getFollowerId())
                    .postId(postId)
                    .createdAt(eventCreatedAt != null ? eventCreatedAt : LocalDateTime.now())
                    .build();
            feedItemsToSave.add(item);
        }

        if (!feedItemsToSave.isEmpty()) {
            feedItemRepository.saveAll(feedItemsToSave);
            log.info("Saved {} feed_items for postId: {}", feedItemsToSave.size(), postId);
        } else {
            log.info("No followers found for authorId: {}; no feed_items generated.", authorId);
        }
    }

    public List<FeedItemResponse> getFeed(UUID userId) {
        return feedItemRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private FeedItemResponse mapToResponse(FeedItem feedItem) {
        return FeedItemResponse.builder()
                .id(feedItem.getId())
                .userId(feedItem.getUserId())
                .postId(feedItem.getPostId())
                .createdAt(feedItem.getCreatedAt())
                .build();
    }
}