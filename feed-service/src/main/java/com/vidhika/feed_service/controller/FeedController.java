package com.vidhika.feed_service.controller;

import com.vidhika.feed_service.dto.FeedItemResponse;
import com.vidhika.feed_service.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    // GET /api/v1/feed
    @GetMapping
    public ResponseEntity<List<FeedItemResponse>> getFeed(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<FeedItemResponse> feed = feedService.getFeed(userId);
        return ResponseEntity.ok(feed);
    }
}
