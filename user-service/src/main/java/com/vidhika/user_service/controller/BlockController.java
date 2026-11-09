package com.vidhika.user_service.controller;

import com.vidhika.user_service.dto.BlockResponse;
import com.vidhika.user_service.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    // POST /api/v1/users/{targetUserId}/block
    @PostMapping("/{targetUserId}/block")
    public ResponseEntity<BlockResponse> blockUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID targetUserId) {

        UUID blockerId = UUID.fromString(jwt.getSubject());
        BlockResponse response = blockService.blockUser(blockerId, targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // DELETE /api/v1/users/{targetUserId}/block
    @DeleteMapping("/{targetUserId}/block")
    public ResponseEntity<Void> unblockUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID targetUserId) {

        UUID blockerId = UUID.fromString(jwt.getSubject());
        blockService.unblockUser(blockerId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/users/blocked (List users blocked by caller)
    @GetMapping("/blocked")
    public ResponseEntity<List<BlockResponse>> getBlockedUsers(@AuthenticationPrincipal Jwt jwt) {
        UUID blockerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(blockService.getBlockedUsers(blockerId));
    }

    // GET /api/v1/users/{userId}/is-blocked/{targetUserId}
    @GetMapping("/{userId}/is-blocked/{targetUserId}")
    public ResponseEntity<Map<String, Boolean>> isBlocked(
            @PathVariable UUID userId,
            @PathVariable UUID targetUserId) {

        boolean blocked = blockService.isBlockedBidirectional(userId, targetUserId);
        return ResponseEntity.ok(Map.of("blocked", blocked));
    }
}
