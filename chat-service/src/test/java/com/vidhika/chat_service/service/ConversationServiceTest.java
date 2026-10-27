package com.vidhika.chat_service.service;

import com.vidhika.chat_service.dto.request.AddMemberRequest;
import com.vidhika.chat_service.dto.request.CreateConversationRequest;
import com.vidhika.chat_service.dto.response.ConversationMemberResponse;
import com.vidhika.chat_service.dto.response.ConversationResponse;
import com.vidhika.chat_service.enums.ConversationType;
import com.vidhika.chat_service.exception.BadRequestException;
import com.vidhika.chat_service.exception.ChatAccessDeniedException;
import com.vidhika.chat_service.exception.ChatNotFoundException;
import com.vidhika.chat_service.model.Conversation;
import com.vidhika.chat_service.model.ConversationMember;
import com.vidhika.chat_service.repository.ConversationMemberRepository;
import com.vidhika.chat_service.repository.ConversationRepository;
import com.vidhika.chat_service.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private String userId1;
    private String userId2;

    @BeforeEach
    void setUp() {
        userId1 = "user-111";
        userId2 = "user-222";
    }

    @Test
    void testCreateOneToOneConversation_Success() {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .type(ConversationType.ONE_TO_ONE)
                .recipientId(userId2)
                .build();

        when(conversationMemberRepository.findByUserId(userId1)).thenReturn(Collections.emptyList());

        Conversation savedConv = Conversation.builder()
                .id("conv-123")
                .type(ConversationType.ONE_TO_ONE)
                .createdBy(userId1)
                .createdAt(LocalDateTime.now())
                .build();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConv);
        when(conversationMemberRepository.findByConversationId("conv-123")).thenReturn(List.of(
                ConversationMember.builder().conversationId("conv-123").userId(userId1).build(),
                ConversationMember.builder().conversationId("conv-123").userId(userId2).build()
        ));

        ConversationResponse response = conversationService.createConversation(userId1, request);

        assertNotNull(response);
        assertEquals("conv-123", response.getId());
        assertEquals(ConversationType.ONE_TO_ONE, response.getType());
        verify(conversationRepository).save(any(Conversation.class));
        verify(conversationMemberRepository).saveAll(anyList());
    }

    @Test
    void testCreateOneToOneConversation_WithSelf_ThrowsBadRequest() {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .type(ConversationType.ONE_TO_ONE)
                .recipientId(userId1)
                .build();

        assertThrows(BadRequestException.class, () -> conversationService.createConversation(userId1, request));
    }

    @Test
    void testCreateGroupConversation_Success() {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .type(ConversationType.GROUP)
                .name("Pulse Hackathon Team")
                .memberIds(List.of(userId2, "user-333"))
                .build();

        Conversation savedConv = Conversation.builder()
                .id("grp-100")
                .type(ConversationType.GROUP)
                .name("Pulse Hackathon Team")
                .createdBy(userId1)
                .createdAt(LocalDateTime.now())
                .build();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConv);
        when(conversationMemberRepository.findByConversationId("grp-100")).thenReturn(List.of(
                ConversationMember.builder().conversationId("grp-100").userId(userId1).build()
        ));

        ConversationResponse response = conversationService.createConversation(userId1, request);

        assertNotNull(response);
        assertEquals("grp-100", response.getId());
        assertEquals("Pulse Hackathon Team", response.getName());
        verify(conversationMemberRepository).saveAll(anyList());
    }

    @Test
    void testGetConversationById_NonMember_ThrowsAccessDenied() {
        Conversation conv = Conversation.builder()
                .id("conv-secret")
                .type(ConversationType.GROUP)
                .build();

        when(conversationRepository.findById("conv-secret")).thenReturn(Optional.of(conv));
        when(conversationMemberRepository.existsByConversationIdAndUserId("conv-secret", userId1)).thenReturn(false);

        assertThrows(ChatAccessDeniedException.class, () -> conversationService.getConversationById(userId1, "conv-secret"));
    }

    @Test
    void testDeleteConversation_NonCreator_ThrowsAccessDenied() {
        Conversation conv = Conversation.builder()
                .id("conv-admin")
                .type(ConversationType.GROUP)
                .createdBy("admin-user")
                .build();

        when(conversationRepository.findById("conv-admin")).thenReturn(Optional.of(conv));

        assertThrows(ChatAccessDeniedException.class, () -> conversationService.deleteConversation(userId1, "conv-admin"));
    }

    @Test
    void testCreateHackathonTeamChat_Idempotent() {
        String teamId = "team-999";
        String hackathonId = "hack-777";

        Conversation existing = Conversation.builder()
                .id("conv-team-999")
                .type(ConversationType.HACKATHON_TEAM)
                .teamId(teamId)
                .hackathonId(hackathonId)
                .name("Team 999")
                .build();

        when(conversationRepository.findByTeamId(teamId)).thenReturn(Optional.of(existing));
        when(conversationMemberRepository.existsByConversationIdAndUserId("conv-team-999", userId1)).thenReturn(true);
        when(conversationMemberRepository.findByConversationId("conv-team-999")).thenReturn(List.of(
                ConversationMember.builder().conversationId("conv-team-999").userId(userId1).build()
        ));

        ConversationResponse response = conversationService.createHackathonTeamChat(
                teamId, hackathonId, List.of(userId1), "Team 999"
        );

        assertNotNull(response);
        assertEquals("conv-team-999", response.getId());
        assertEquals(teamId, response.getTeamId());
        // Verify no duplicate conversation was saved
        verify(conversationRepository, never()).save(any(Conversation.class));
    }
}
