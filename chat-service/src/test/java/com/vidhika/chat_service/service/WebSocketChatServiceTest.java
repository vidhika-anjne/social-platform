package com.vidhika.chat_service.service;

import com.vidhika.chat_service.dto.request.MessageStatusUpdateRequest;
import com.vidhika.chat_service.dto.request.SendMessageRequest;
import com.vidhika.chat_service.dto.request.TypingRequest;
import com.vidhika.chat_service.dto.response.MessageResponse;
import com.vidhika.chat_service.enums.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketChatServiceTest {

    @Mock
    private MessageService messageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketChatServiceImpl webSocketChatService;

    private String senderId;
    private String conversationId;

    @BeforeEach
    void setUp() {
        senderId = "user-100";
        conversationId = "conv-500";
    }

    @Test
    void testProcessAndBroadcastMessage() {
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Realtime test message")
                .build();

        MessageResponse response = MessageResponse.builder()
                .id("msg-888")
                .conversationId(conversationId)
                .senderId(senderId)
                .content("Realtime test message")
                .status(MessageStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();

        when(messageService.saveMessage(senderId, conversationId, "Realtime test message")).thenReturn(response);

        MessageResponse result = webSocketChatService.processAndBroadcastMessage(senderId, conversationId, request);

        assertNotNull(result);
        assertEquals("msg-888", result.getId());
        verify(messageService).saveMessage(senderId, conversationId, "Realtime test message");
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + conversationId), eq(response));
    }

    @Test
    void testBroadcastTyping() {
        TypingRequest request = TypingRequest.builder().typing(true).build();

        webSocketChatService.broadcastTyping(senderId, conversationId, request);

        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + conversationId + "/typing"), any(Map.class));
    }

    @Test
    void testBroadcastDelivered() {
        MessageStatusUpdateRequest request = MessageStatusUpdateRequest.builder()
                .messageId("msg-888")
                .build();

        webSocketChatService.broadcastDelivered(senderId, conversationId, request);

        verify(messageService).updateMessageStatus(senderId, conversationId, "msg-888", MessageStatus.DELIVERED);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + conversationId + "/delivered"), any(Map.class));
    }

    @Test
    void testBroadcastRead() {
        MessageStatusUpdateRequest request = MessageStatusUpdateRequest.builder()
                .messageId("msg-888")
                .build();

        webSocketChatService.broadcastRead(senderId, conversationId, request);

        verify(messageService).updateMessageStatus(senderId, conversationId, "msg-888", MessageStatus.READ);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + conversationId + "/read"), any(Map.class));
    }
}
