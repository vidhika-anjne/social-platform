package com.vidhika.chat_service.service;

import com.vidhika.chat_service.dto.request.AddMemberRequest;
import com.vidhika.chat_service.dto.request.CreateConversationRequest;
import com.vidhika.chat_service.dto.response.ConversationMemberResponse;
import com.vidhika.chat_service.dto.response.ConversationResponse;

import java.util.List;

public interface ConversationService {

    ConversationResponse createConversation(String currentUserId, CreateConversationRequest request);

    List<ConversationResponse> getUserConversations(String currentUserId);

    ConversationResponse getConversationById(String currentUserId, String conversationId);

    void deleteConversation(String currentUserId, String conversationId);

    List<ConversationMemberResponse> getConversationMembers(String currentUserId, String conversationId);

    List<ConversationMemberResponse> addMembers(String currentUserId, String conversationId, AddMemberRequest request);

    void removeMember(String currentUserId, String conversationId, String targetUserId);

    ConversationResponse createHackathonTeamChat(String teamId, String hackathonId, List<String> memberIds, String teamName);
}