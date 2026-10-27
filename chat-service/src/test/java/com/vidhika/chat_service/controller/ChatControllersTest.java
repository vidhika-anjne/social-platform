package com.vidhika.chat_service.controller;

import com.vidhika.chat_service.dto.request.AddMemberRequest;
import com.vidhika.chat_service.dto.request.CreateConversationRequest;
import com.vidhika.chat_service.dto.response.ConversationMemberResponse;
import com.vidhika.chat_service.dto.response.ConversationResponse;
import com.vidhika.chat_service.dto.response.MessageResponse;
import com.vidhika.chat_service.enums.ConversationType;
import com.vidhika.chat_service.enums.MessageStatus;
import com.vidhika.chat_service.service.ConversationService;
import com.vidhika.chat_service.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllersTest {

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private ConversationController conversationController;

    @InjectMocks
    private MessageController messageController;

    private Jwt mockJwt;
    private final String userId = "user-123";

    @BeforeEach
    void setUp() {
        mockJwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS384"),
                Map.of("sub", userId, "email", "test@pulse.com")
        );
    }

    @Test
    void testCreateConversation_Endpoint() {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .type(ConversationType.GROUP)
                .name("General")
                .memberIds(List.of("u1", "u2"))
                .build();

        ConversationResponse response = ConversationResponse.builder()
                .id("conv-1")
                .type(ConversationType.GROUP)
                .name("General")
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        when(conversationService.createConversation(eq(userId), any(CreateConversationRequest.class)))
                .thenReturn(response);

        ResponseEntity<ConversationResponse> result = conversationController.createConversation(mockJwt, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("conv-1", result.getBody().getId());
        assertEquals("General", result.getBody().getName());
    }

    @Test
    void testGetUserConversations_Endpoint() {
        ConversationResponse response = ConversationResponse.builder()
                .id("conv-1")
                .type(ConversationType.ONE_TO_ONE)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        when(conversationService.getUserConversations(userId)).thenReturn(List.of(response));

        ResponseEntity<List<ConversationResponse>> result = conversationController.getUserConversations(mockJwt);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals("conv-1", result.getBody().get(0).getId());
    }

    @Test
    void testGetConversationById_Endpoint() {
        ConversationResponse response = ConversationResponse.builder()
                .id("conv-99")
                .type(ConversationType.GROUP)
                .name("Team Chat")
                .build();

        when(conversationService.getConversationById(userId, "conv-99")).thenReturn(response);

        ResponseEntity<ConversationResponse> result = conversationController.getConversation(mockJwt, "conv-99");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("conv-99", result.getBody().getId());
    }

    @Test
    void testDeleteConversation_Endpoint() {
        doNothing().when(conversationService).deleteConversation(userId, "conv-99");

        ResponseEntity<Void> result = conversationController.deleteConversation(mockJwt, "conv-99");

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(conversationService).deleteConversation(userId, "conv-99");
    }

    @Test
    void testGetMembers_Endpoint() {
        ConversationMemberResponse member = ConversationMemberResponse.builder()
                .id("m-1")
                .conversationId("conv-1")
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();

        when(conversationService.getConversationMembers(userId, "conv-1")).thenReturn(List.of(member));

        ResponseEntity<List<ConversationMemberResponse>> result = conversationController.getMembers(mockJwt, "conv-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals(userId, result.getBody().get(0).getUserId());
    }

    @Test
    void testAddMembers_Endpoint() {
        AddMemberRequest request = AddMemberRequest.builder()
                .userId("new-user")
                .build();

        ConversationMemberResponse member = ConversationMemberResponse.builder()
                .id("m-2")
                .conversationId("conv-1")
                .userId("new-user")
                .build();

        when(conversationService.addMembers(eq(userId), eq("conv-1"), any(AddMemberRequest.class)))
                .thenReturn(List.of(member));

        ResponseEntity<List<ConversationMemberResponse>> result = conversationController.addMembers(mockJwt, "conv-1", request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals("new-user", result.getBody().get(0).getUserId());
    }

    @Test
    void testRemoveMember_Endpoint() {
        doNothing().when(conversationService).removeMember(userId, "conv-1", "target-user");

        ResponseEntity<Void> result = conversationController.removeMember(mockJwt, "conv-1", "target-user");

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(conversationService).removeMember(userId, "conv-1", "target-user");
    }

    @Test
    void testGetMessages_Endpoint() {
        MessageResponse msg = MessageResponse.builder()
                .id("msg-1")
                .conversationId("conv-1")
                .senderId(userId)
                .content("Hello World")
                .status(MessageStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        when(messageService.getMessageHistory(eq(userId), eq("conv-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(msg)));

        ResponseEntity<Page<MessageResponse>> result = messageController.getMessages(mockJwt, "conv-1", 0, 10, "desc");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getTotalElements());
        assertEquals("Hello World", result.getBody().getContent().get(0).getContent());
    }
}
