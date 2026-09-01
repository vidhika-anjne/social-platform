package com.vidhika.user_service.service;

import com.vidhika.user_service.dto.CountResponse;
import com.vidhika.user_service.dto.FollowResponse;
import com.vidhika.user_service.exception.AlreadyFollowingException;
import com.vidhika.user_service.exception.UserNotFoundException;
import com.vidhika.user_service.model.Follow;
import com.vidhika.user_service.repository.FollowRepository;
import com.vidhika.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserProfileRepository userProfileRepository;

    public FollowResponse follow(UUID followerId, UUID targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new RuntimeException("You cannot follow yourself");
        }

        if (!userProfileRepository.existsById(targetUserId)) {
            throw new UserNotFoundException(targetUserId.toString());
        }

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, targetUserId)) {
            throw new AlreadyFollowingException();
        }

        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(targetUserId)
                .build();

        Follow saved = followRepository.save(follow);
        log.info("User {} followed user {}", followerId, targetUserId);
        return mapToResponse(saved);
    }

    public void unfollow(UUID followerId, UUID targetUserId) {
        Follow follow = followRepository
                .findByFollowerIdAndFollowingId(followerId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Follow relationship not found"));

        followRepository.delete(follow);
        log.info("User {} unfollowed user {}", followerId, targetUserId);
    }

    public List<FollowResponse> getFollowers(UUID userId) {
        return followRepository.findByFollowingId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<FollowResponse> getFollowing(UUID userId) {
        return followRepository.findByFollowerId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CountResponse getFollowersCount(UUID userId) {
        return CountResponse.builder()
                .count(followRepository.countByFollowingId(userId))
                .build();
    }

    public CountResponse getFollowingCount(UUID userId) {
        return CountResponse.builder()
                .count(followRepository.countByFollowerId(userId))
                .build();
    }

    private FollowResponse mapToResponse(Follow follow) {
        return FollowResponse.builder()
                .followerId(follow.getFollowerId())
                .followingId(follow.getFollowingId())
                .createdAt(follow.getCreatedAt())
                .build();
    }
}
