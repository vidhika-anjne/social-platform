package com.vidhika.chat_service.service;

import com.vidhika.chat_service.dto.request.AddMemberRequest;
import com.vidhika.chat_service.dto.request.CreateConversationRequest;
import com.vidhika.chat_service.dto.response.ConversationMemberResponse;
import com.vidhika.chat_service.dto.response.ConversationResponse;
import com.vidhika.chat_service.dto.response.MessageResponse;
import com.vidhika.chat_service.enums.ConversationType;
import com.vidhika.chat_service.exception.BadRequestException;
import com.vidhika.chat_service.exception.ChatAccessDeniedException;
import com.vidhika.chat_service.exception.ChatNotFoundException;
import com.vidhika.chat_service.model.Conversation;
import com.vidhika.chat_service.model.ConversationMember;
import com.vidhika.chat_service.model.Message;
import com.vidhika.chat_service.repository.ConversationMemberRepository;
import com.vidhika.chat_service.repository.ConversationRepository;
import com.vidhika.chat_service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public ConversationResponse createConversation(String currentUserId, CreateConversationRequest request) {
        if (request.getType() == null) {
            throw new BadRequestException("Conversation type is required");
        }

        if (request.getType() == ConversationType.ONE_TO_ONE) {
            String recipientId = request.getRecipientId();
            if (recipientId == null || recipientId.isBlank()) {
                if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
                    recipientId = request.getMemberIds().get(0);
                }
            }
            if (recipientId == null || recipientId.isBlank()) {
                throw new BadRequestException("Recipient ID is required for 1-to-1 conversation");
            }
            if (recipientId.equals(currentUserId)) {
                throw new BadRequestException("Cannot create a 1-to-1 conversation with yourself");
            }

            // Check if 1-to-1 conversation already exists
            Optional<Conversation> existingConv = findExistingOneToOne(currentUserId, recipientId);
            if (existingConv.isPresent()) {
                log.info("Returning existing 1-to-1 conversation between {} and {}", currentUserId, recipientId);
                return mapToResponse(existingConv.get());
            }

            // Create new 1-to-1 conversation
            Conversation conversation = Conversation.builder()
                    .type(ConversationType.ONE_TO_ONE)
                    .name(request.getName())
                    .createdBy(currentUserId)
                    .createdAt(LocalDateTime.now())
                    .build();

            conversation = conversationRepository.save(conversation);

            // Add members
            ConversationMember member1 = ConversationMember.builder()
                    .conversationId(conversation.getId())
                    .userId(currentUserId)
                    .joinedAt(LocalDateTime.now())
                    .build();

            ConversationMember member2 = ConversationMember.builder()
                    .conversationId(conversation.getId())
                    .userId(recipientId)
                    .joinedAt(LocalDateTime.now())
                    .build();

            conversationMemberRepository.saveAll(List.of(member1, member2));
            log.info("Created 1-to-1 conversation {} for users {} and {}", conversation.getId(), currentUserId, recipientId);

            return mapToResponse(conversation);

        } else if (request.getType() == ConversationType.GROUP) {
            if (request.getName() == null || request.getName().isBlank()) {
                throw new BadRequestException("Group conversation name is required");
            }

            Conversation conversation = Conversation.builder()
                    .type(ConversationType.GROUP)
                    .name(request.getName().trim())
                    .hackathonId(request.getHackathonId())
                    .createdBy(currentUserId)
                    .createdAt(LocalDateTime.now())
                    .build();

            conversation = conversationRepository.save(conversation);

            // Collect all unique members
            Set<String> memberIds = new LinkedHashSet<>();
            memberIds.add(currentUserId);
            if (request.getMemberIds() != null) {
                memberIds.addAll(request.getMemberIds());
            }

            Conversation savedConversation = conversation;
            List<ConversationMember> members = memberIds.stream()
                    .filter(uid -> uid != null && !uid.isBlank())
                    .map(uid -> ConversationMember.builder()
                            .conversationId(savedConversation.getId())
                            .userId(uid.trim())
                            .joinedAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());

            conversationMemberRepository.saveAll(members);
            log.info("Created GROUP conversation {} with {} members", conversation.getId(), members.size());

            return mapToResponse(conversation);

        } else {
            throw new BadRequestException("Direct creation of " + request.getType() + " conversation is not supported via this endpoint");
        }
    }

    @Override
    public List<ConversationResponse> getUserConversations(String currentUserId) {
        List<ConversationMember> memberships = conversationMemberRepository.findByUserId(currentUserId);
        if (memberships.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> convIds = memberships.stream()
                .map(ConversationMember::getConversationId)
                .collect(Collectors.toList());

        List<Conversation> conversations = conversationRepository.findByIdIn(convIds);

        return conversations.stream()
                .map(this::mapToResponse)
                .sorted((c1, c2) -> {
                    LocalDateTime t1 = c1.getLastMessage() != null ? c1.getLastMessage().getCreatedAt() : c1.getCreatedAt();
                    LocalDateTime t2 = c2.getLastMessage() != null ? c2.getLastMessage().getCreatedAt() : c2.getCreatedAt();
                    if (t1 == null || t2 == null) return 0;
                    return t2.compareTo(t1);
                })
                .collect(Collectors.toList());
    }

    @Override
    public ConversationResponse getConversationById(String currentUserId, String conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ChatNotFoundException("Conversation not found with id: " + conversationId));

        boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId);
        if (!isMember) {
            throw new ChatAccessDeniedException("Access denied: You are not a member of this conversation");
        }

        return mapToResponse(conversation);
    }

    @Override
    @Transactional
    public void deleteConversation(String currentUserId, String conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ChatNotFoundException("Conversation not found with id: " + conversationId));

        if (!currentUserId.equals(conversation.getCreatedBy())) {
            throw new ChatAccessDeniedException("Access denied: Only the creator can delete this conversation");
        }

        messageRepository.deleteByConversationId(conversationId);
        conversationMemberRepository.deleteByConversationId(conversationId);
        conversationRepository.deleteById(conversationId);
        log.info("Deleted conversation {} and associated members/messages by user {}", conversationId, currentUserId);
    }

    @Override
    public List<ConversationMemberResponse> getConversationMembers(String currentUserId, String conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new ChatNotFoundException("Conversation not found with id: " + conversationId);
        }

        boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId);
        if (!isMember) {
            throw new ChatAccessDeniedException("Access denied: You are not a member of this conversation");
        }

        return conversationMemberRepository.findByConversationId(conversationId).stream()
                .map(this::mapMemberToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ConversationMemberResponse> addMembers(String currentUserId, String conversationId, AddMemberRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ChatNotFoundException("Conversation not found with id: " + conversationId));

        boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, currentUserId);
        if (!isMember) {
            throw new ChatAccessDeniedException("Access denied: You are not a member of this conversation");
        }

        if (conversation.getType() == ConversationType.ONE_TO_ONE) {
            throw new BadRequestException("Cannot add members to a 1-to-1 conversation");
        }

        Set<String> targetUserIds = new LinkedHashSet<>();
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            targetUserIds.add(request.getUserId().trim());
        }
        if (request.getUserIds() != null) {
            targetUserIds.addAll(request.getUserIds().stream().filter(id -> id != null && !id.isBlank()).map(String::trim).toList());
        }

        if (targetUserIds.isEmpty()) {
            throw new BadRequestException("No user IDs provided to add");
        }

        List<ConversationMember> added = new ArrayList<>();
        for (String uid : targetUserIds) {
            if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, uid)) {
                ConversationMember newMember = ConversationMember.builder()
                        .conversationId(conversationId)
                        .userId(uid)
                        .joinedAt(LocalDateTime.now())
                        .build();
                added.add(conversationMemberRepository.save(newMember));
            }
        }

        log.info("Added {} new members to conversation {}", added.size(), conversationId);
        return conversationMemberRepository.findByConversationId(conversationId).stream()
                .map(this::mapMemberToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeMember(String currentUserId, String conversationId, String targetUserId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ChatNotFoundException("Conversation not found with id: " + conversationId));

        // Allowed if current user is removing themselves (leaving) OR if current user is the conversation creator
        boolean isSelf = currentUserId.equals(targetUserId);
        boolean isCreator = currentUserId.equals(conversation.getCreatedBy());

        if (!isSelf && !isCreator) {
            throw new ChatAccessDeniedException("Access denied: You do not have permission to remove this member");
        }

        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, targetUserId)) {
            throw new ChatNotFoundException("Member " + targetUserId + " not found in conversation " + conversationId);
        }

        conversationMemberRepository.deleteByConversationIdAndUserId(conversationId, targetUserId);
        log.info("Removed member {} from conversation {} by user {}", targetUserId, conversationId, currentUserId);
    }

    @Override
    @Transactional
    public ConversationResponse createHackathonTeamChat(String teamId, String hackathonId, List<String> memberIds, String teamName) {
        if (teamId == null || teamId.isBlank()) {
            throw new BadRequestException("Team ID cannot be null or blank");
        }

        // Idempotency check: see if a conversation for this team already exists
        Optional<Conversation> existing = conversationRepository.findByTeamId(teamId);
        if (existing.isPresent()) {
            log.info("Hackathon team chat already exists for teamId: {}. Ensuring all members are present.", teamId);
            Conversation conv = existing.get();
            if (memberIds != null) {
                for (String memberId : memberIds) {
                    if (memberId != null && !memberId.isBlank() && !conversationMemberRepository.existsByConversationIdAndUserId(conv.getId(), memberId.trim())) {
                        conversationMemberRepository.save(ConversationMember.builder()
                                .conversationId(conv.getId())
                                .userId(memberId.trim())
                                .joinedAt(LocalDateTime.now())
                                .build());
                    }
                }
            }
            return mapToResponse(conv);
        }

        String displayName = (teamName != null && !teamName.isBlank()) ? teamName : "Team Chat";
        String creatorId = (memberIds != null && !memberIds.isEmpty()) ? memberIds.get(0) : "SYSTEM";

        Conversation conversation = Conversation.builder()
                .type(ConversationType.HACKATHON_TEAM)
                .name(displayName)
                .hackathonId(hackathonId)
                .teamId(teamId)
                .createdBy(creatorId)
                .createdAt(LocalDateTime.now())
                .build();

        conversation = conversationRepository.save(conversation);

        if (memberIds != null) {
            Conversation savedConv = conversation;
            List<ConversationMember> members = memberIds.stream()
                    .filter(uid -> uid != null && !uid.isBlank())
                    .distinct()
                    .map(uid -> ConversationMember.builder()
                            .conversationId(savedConv.getId())
                            .userId(uid.trim())
                            .joinedAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());

            conversationMemberRepository.saveAll(members);
            log.info("Created Hackathon Team conversation {} for teamId {} with {} members", conversation.getId(), teamId, members.size());
        }

        return mapToResponse(conversation);
    }

    private Optional<Conversation> findExistingOneToOne(String user1, String user2) {
        List<ConversationMember> user1Memberships = conversationMemberRepository.findByUserId(user1);
        if (user1Memberships.isEmpty()) return Optional.empty();

        Set<String> user1ConvIds = user1Memberships.stream()
                .map(ConversationMember::getConversationId)
                .collect(Collectors.toSet());

        List<ConversationMember> user2Memberships = conversationMemberRepository.findByUserId(user2);
        for (ConversationMember m : user2Memberships) {
            if (user1ConvIds.contains(m.getConversationId())) {
                Optional<Conversation> c = conversationRepository.findById(m.getConversationId());
                if (c.isPresent() && c.get().getType() == ConversationType.ONE_TO_ONE) {
                    return c;
                }
            }
        }

        return Optional.empty();
    }

    private ConversationResponse mapToResponse(Conversation conv) {
        List<ConversationMemberResponse> members = conversationMemberRepository.findByConversationId(conv.getId()).stream()
                .map(this::mapMemberToResponse)
                .collect(Collectors.toList());

        MessageResponse lastMsg = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conv.getId())
                .map(this::mapMessageToResponse)
                .orElse(null);

        return ConversationResponse.builder()
                .id(conv.getId())
                .type(conv.getType())
                .name(conv.getName())
                .hackathonId(conv.getHackathonId())
                .teamId(conv.getTeamId())
                .createdBy(conv.getCreatedBy())
                .createdAt(conv.getCreatedAt())
                .members(members)
                .lastMessage(lastMsg)
                .build();
    }

    private ConversationMemberResponse mapMemberToResponse(ConversationMember member) {
        return ConversationMemberResponse.builder()
                .id(member.getId())
                .conversationId(member.getConversationId())
                .userId(member.getUserId())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private MessageResponse mapMessageToResponse(Message msg) {
        return MessageResponse.builder()
                .id(msg.getId())
                .conversationId(msg.getConversationId())
                .senderId(msg.getSenderId())
                .content(msg.getContent())
                .createdAt(msg.getCreatedAt())
                .status(msg.getStatus())
                .build();
    }
}
