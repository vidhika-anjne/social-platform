package com.vidhika.chat_service.service;

import com.vidhika.chat_service.dto.response.MessageResponse;
import com.vidhika.chat_service.enums.MessageStatus;
import com.vidhika.chat_service.exception.BadRequestException;
import com.vidhika.chat_service.exception.ChatAccessDeniedException;
import com.vidhika.chat_service.exception.ChatNotFoundException;
import com.vidhika.chat_service.model.Message;
import com.vidhika.chat_service.repository.ConversationMemberRepository;
import com.vidhika.chat_service.repository.ConversationRepository;
import com.vidhika.chat_service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;

    @Override
    public Page<MessageResponse> getMessageHistory(String currentUserId, String conversationId, Pageable pageable) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new ChatNotFoundException("Conversation not found with id: " + conversationId);
        }

        boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId);
        if (!isMember) {
            throw new ChatAccessDeniedException("Access denied: You are not a member of conversation " + conversationId);
        }

        Page<Message> messagePage = messageRepository.findByConversationId(conversationId, pageable);
        return messagePage.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public MessageResponse saveMessage(String senderId, String conversationId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BadRequestException("Message content cannot be empty");
        }

        if (!conversationRepository.existsById(conversationId)) {
            throw new ChatNotFoundException("Conversation not found with id: " + conversationId);
        }

        boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, senderId);
        if (!isMember) {
            throw new ChatAccessDeniedException("Access denied: You are not a member of conversation " + conversationId);
        }

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content.trim())
                .createdAt(LocalDateTime.now())
                .status(MessageStatus.SENT)
                .build();

        message = messageRepository.save(message);
        log.info("Persisted message {} for conversation {} from sender {}", message.getId(), conversationId, senderId);

        return mapToResponse(message);
    }

    @Override
    @Transactional
    public MessageResponse updateMessageStatus(String currentUserId, String conversationId, String messageId, MessageStatus status) {
        boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId);
        if (!isMember) {
            throw new ChatAccessDeniedException("Access denied: You are not a member of conversation " + conversationId);
        }

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ChatNotFoundException("Message not found with id: " + messageId));

        if (!message.getConversationId().equals(conversationId)) {
            throw new BadRequestException("Message does not belong to conversation " + conversationId);
        }

        // Only allow advancement of status (e.g., SENT -> DELIVERED -> READ)
        if (message.getStatus() != MessageStatus.READ) {
            message.setStatus(status);
            message = messageRepository.save(message);
            log.debug("Updated message {} status to {}", messageId, status);
        }

        return mapToResponse(message);
    }

    @Override
    @Transactional
    public void markMessagesAsDelivered(String currentUserId, String conversationId) {
        boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId);
        if (!isMember) {
            throw new ChatAccessDeniedException("Access denied: You are not a member of conversation " + conversationId);
        }

        List<Message> pending = messageRepository.findByConversationIdAndStatusNotAndSenderIdNot(
                conversationId, MessageStatus.READ, currentUserId
        );

        for (Message msg : pending) {
            if (msg.getStatus() == MessageStatus.SENT) {
                msg.setStatus(MessageStatus.DELIVERED);
            }
        }
        messageRepository.saveAll(pending);
    }

    @Override
    @Transactional
    public void markMessagesAsRead(String currentUserId, String conversationId) {
        boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId);
        if (!isMember) {
            throw new ChatAccessDeniedException("Access denied: You are not a member of conversation " + conversationId);
        }

        List<Message> unread = messageRepository.findByConversationIdAndStatusNotAndSenderIdNot(
                conversationId, MessageStatus.READ, currentUserId
        );

        for (Message msg : unread) {
            msg.setStatus(MessageStatus.READ);
        }
        messageRepository.saveAll(unread);
    }

    private MessageResponse mapToResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .status(message.getStatus())
                .build();
    }
}
