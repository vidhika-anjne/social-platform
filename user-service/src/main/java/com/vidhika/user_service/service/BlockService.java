package com.vidhika.user_service.service;

import com.vidhika.user_service.dto.BlockResponse;
import com.vidhika.user_service.exception.UserNotFoundException;
import com.vidhika.user_service.model.UserBlock;
import com.vidhika.user_service.repository.FollowRepository;
import com.vidhika.user_service.repository.UserBlockRepository;
import com.vidhika.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockService {

    private final UserBlockRepository blockRepository;
    private final UserProfileRepository profileRepository;
    private final FollowRepository followRepository;

    @Transactional
    public BlockResponse blockUser(UUID blockerId, UUID targetUserId) {
        if (blockerId.equals(targetUserId)) {
            throw new RuntimeException("You cannot block yourself");
        }

        if (!profileRepository.existsById(targetUserId)) {
            throw new UserNotFoundException(targetUserId.toString());
        }

        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId)) {
            log.info("User {} already blocked user {}", blockerId, targetUserId);
            UserBlock existing = blockRepository.findByBlockerIdAndBlockedId(blockerId, targetUserId).orElseThrow();
            return mapToResponse(existing);
        }

        UserBlock block = UserBlock.builder()
                .blockerId(blockerId)
                .blockedId(targetUserId)
                .build();

        UserBlock saved = blockRepository.save(block);

        // Remove follow relationships in both directions
        followRepository.findByFollowerIdAndFollowingId(blockerId, targetUserId)
                .ifPresent(followRepository::delete);
        followRepository.findByFollowerIdAndFollowingId(targetUserId, blockerId)
                .ifPresent(followRepository::delete);

        log.info("User {} blocked user {}", blockerId, targetUserId);
        return mapToResponse(saved);
    }

    @Transactional
    public void unblockUser(UUID blockerId, UUID targetUserId) {
        blockRepository.deleteByBlockerIdAndBlockedId(blockerId, targetUserId);
        log.info("User {} unblocked user {}", blockerId, targetUserId);
    }

    public boolean isBlocked(UUID blockerId, UUID targetUserId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, targetUserId);
    }

    public boolean isBlockedBidirectional(UUID userA, UUID userB) {
        return blockRepository.existsByBlockerIdAndBlockedId(userA, userB)
                || blockRepository.existsByBlockerIdAndBlockedId(userB, userA);
    }

    public List<BlockResponse> getBlockedUsers(UUID blockerId) {
        return blockRepository.findByBlockerId(blockerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BlockResponse mapToResponse(UserBlock block) {
        return BlockResponse.builder()
                .id(block.getId())
                .blockerId(block.getBlockerId())
                .blockedId(block.getBlockedId())
                .createdAt(block.getCreatedAt())
                .build();
    }
}

