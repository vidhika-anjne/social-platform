package com.vidhika.chat_service.controller;

import com.vidhika.chat_service.dto.response.MessageResponse;
import com.vidhika.chat_service.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // GET /api/v1/chats/{conversationId}/messages (Paginated message history)
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "desc") String direction) {

        String currentUserId = jwt.getSubject();
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(sortDirection, "createdAt"));

        Page<MessageResponse> messagePage = messageService.getMessageHistory(currentUserId, conversationId, pageable);
        return ResponseEntity.ok(messagePage);
    }
}
