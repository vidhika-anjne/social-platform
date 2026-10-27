package com.vidhika.chat_service.controller;

import com.vidhika.chat_service.dto.request.AddMemberRequest;
import com.vidhika.chat_service.dto.request.CreateConversationRequest;
import com.vidhika.chat_service.dto.response.ConversationMemberResponse;
import com.vidhika.chat_service.dto.response.ConversationResponse;
import com.vidhika.chat_service.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    // POST /api/v1/chats (Create 1-to-1 or GROUP conversation)
    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateConversationRequest request) {

        String currentUserId = jwt.getSubject();
        log.info("Creating conversation of type {} by user {}", request.getType(), currentUserId);
        ConversationResponse response = conversationService.createConversation(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/v1/chats (List user's active conversations)
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getUserConversations(
            @AuthenticationPrincipal Jwt jwt) {

        String currentUserId = jwt.getSubject();
        List<ConversationResponse> conversations = conversationService.getUserConversations(currentUserId);
        return ResponseEntity.ok(conversations);
    }

    // GET /api/v1/chats/{conversationId} (Get conversation details)
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String conversationId) {

        String currentUserId = jwt.getSubject();
        ConversationResponse response = conversationService.getConversationById(currentUserId, conversationId);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/v1/chats/{conversationId} (Delete conversation)
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String conversationId) {

        String currentUserId = jwt.getSubject();
        conversationService.deleteConversation(currentUserId, conversationId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/chats/{conversationId}/members (Get conversation members)
    @GetMapping("/{conversationId}/members")
    public ResponseEntity<List<ConversationMemberResponse>> getMembers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String conversationId) {

        String currentUserId = jwt.getSubject();
        List<ConversationMemberResponse> members = conversationService.getConversationMembers(currentUserId, conversationId);
        return ResponseEntity.ok(members);
    }

    // POST /api/v1/chats/{conversationId}/members (Add member(s) to conversation)
    @PostMapping("/{conversationId}/members")
    public ResponseEntity<List<ConversationMemberResponse>> addMembers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String conversationId,
            @RequestBody AddMemberRequest request) {

        String currentUserId = jwt.getSubject();
        List<ConversationMemberResponse> updatedMembers = conversationService.addMembers(currentUserId, conversationId, request);
        return ResponseEntity.ok(updatedMembers);
    }

    // DELETE /api/v1/chats/{conversationId}/members/{userId} (Remove member or leave chat)
    @DeleteMapping("/{conversationId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String conversationId,
            @PathVariable String userId) {

        String currentUserId = jwt.getSubject();
        conversationService.removeMember(currentUserId, conversationId, userId);
        return ResponseEntity.noContent().build();
    }
}