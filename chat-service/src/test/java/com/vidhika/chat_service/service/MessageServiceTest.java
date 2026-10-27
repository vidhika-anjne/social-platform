package com.vidhika.chat_service.service;

import com.vidhika.chat_service.dto.response.MessageResponse;
import com.vidhika.chat_service.enums.MessageStatus;
import com.vidhika.chat_service.exception.ChatAccessDeniedException;
import com.vidhika.chat_service.model.Message;
import com.vidhika.chat_service.repository.ConversationMemberRepository;
import com.vidhika.chat_service.repository.ConversationRepository;
import com.vidhika.chat_service.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMemberRepository conversationMemberRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private String senderId;
    private String conversationId;

    @BeforeEach
    void setUp() {
        senderId = "user-101";
        conversationId = "conv-abc";
    }

    @Test
    void testSaveMessage_Success() {
        when(conversationRepository.existsById(conversationId)).thenReturn(true);
        when(conversationMemberRepository.existsByConversationIdAndUserId(conversationId, senderId)).thenReturn(true);

        Message savedMsg = Message.builder()
                .id("msg-1")
                .conversationId(conversationId)
                .senderId(senderId)
                .content("Hello World")
                .status(MessageStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

        MessageResponse response = messageService.saveMessage(senderId, conversationId, "Hello World");

        assertNotNull(response);
        assertEquals("msg-1", response.getId());
        assertEquals("Hello World", response.getContent());
        assertEquals(MessageStatus.SENT, response.getStatus());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void testSaveMessage_NonMember_ThrowsAccessDenied() {
        when(conversationRepository.existsById(conversationId)).thenReturn(true);
        when(conversationMemberRepository.existsByConversationIdAndUserId(conversationId, senderId)).thenReturn(false);

        assertThrows(ChatAccessDeniedException.class, () ->
                messageService.saveMessage(senderId, conversationId, "Unauthorized message"));
    }

    @Test
    void testGetMessageHistory_Success() {
        when(conversationRepository.existsById(conversationId)).thenReturn(true);
        when(conversationMemberRepository.existsByConversationIdAndUserId(conversationId, senderId)).thenReturn(true);

        Message msg1 = Message.builder()
                .id("m1")
                .conversationId(conversationId)
                .senderId(senderId)
                .content("Msg 1")
                .createdAt(LocalDateTime.now())
                .status(MessageStatus.SENT)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(messageRepository.findByConversationId(conversationId, pageable))
                .thenReturn(new PageImpl<>(List.of(msg1)));

        Page<MessageResponse> page = messageService.getMessageHistory(senderId, conversationId, pageable);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("Msg 1", page.getContent().get(0).getContent());
    }

    @Test
    void testUpdateMessageStatus_ToRead() {
        when(conversationMemberRepository.existsByConversationIdAndUserId(conversationId, senderId)).thenReturn(true);

        Message msg = Message.builder()
                .id("m-read")
                .conversationId(conversationId)
                .senderId("other-user")
                .content("Test")
                .status(MessageStatus.DELIVERED)
                .build();

        when(messageRepository.findById("m-read")).thenReturn(Optional.of(msg));
        when(messageRepository.save(any(Message.class))).thenAnswer(i -> i.getArgument(0));

        MessageResponse response = messageService.updateMessageStatus(senderId, conversationId, "m-read", MessageStatus.READ);

        assertNotNull(response);
        assertEquals(MessageStatus.READ, response.getStatus());
    }
}
