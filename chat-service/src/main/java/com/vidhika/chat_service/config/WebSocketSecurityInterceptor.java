package com.vidhika.chat_service.config;

import com.vidhika.chat_service.repository.ConversationMemberRepository;
import com.vidhika.chat_service.security.JwtTokenProvider;
import com.vidhika.chat_service.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ConversationMemberRepository conversationMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && accessor.getCommand() != null) {
            StompCommand command = accessor.getCommand();

            if (StompCommand.CONNECT.equals(command)) {
                handleConnect(accessor);
            } else if (StompCommand.SUBSCRIBE.equals(command)) {
                handleSubscribe(accessor);
            } else if (StompCommand.SEND.equals(command)) {
                handleSend(accessor);
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null) {
            authHeader = accessor.getFirstNativeHeader("token");
        }

        if (authHeader == null || authHeader.isBlank()) {
            log.error("WebSocket connection rejected: Missing Authorization header");
            throw new AccessDeniedException("Missing Authorization header in STOMP connect");
        }

        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();

        if (!jwtTokenProvider.validateToken(token)) {
            log.error("WebSocket connection rejected: Invalid or expired JWT token");
            throw new AccessDeniedException("Invalid or expired JWT token");
        }

        String userId = jwtTokenProvider.extractUserId(token);
        String email = jwtTokenProvider.extractEmail(token);
        String role = jwtTokenProvider.extractRole(token);

        UserPrincipal principal = new UserPrincipal(userId, email, role);
        accessor.setUser(principal);
        log.info("WebSocket connected successfully for user: {}", userId);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            log.error("Subscription rejected: Unauthenticated user");
            throw new AccessDeniedException("Authentication required to subscribe");
        }

        String destination = accessor.getDestination();
        if (destination != null && destination.startsWith("/topic/chat/")) {
            String conversationId = extractConversationId(destination, "/topic/chat/");
            if (conversationId != null && !conversationId.isBlank()) {
                boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, user.getName());
                if (!isMember) {
                    log.warn("User {} attempted unauthorized subscription to conversation {}", user.getName(), conversationId);
                    throw new AccessDeniedException("User is not a member of conversation " + conversationId);
                }
                log.debug("User {} authorized to subscribe to {}", user.getName(), destination);
            }
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            log.error("Message send rejected: Unauthenticated user");
            throw new AccessDeniedException("Authentication required to send messages");
        }

        String destination = accessor.getDestination();
        if (destination != null && destination.startsWith("/app/chat/")) {
            String conversationId = extractConversationId(destination, "/app/chat/");
            if (conversationId != null && !conversationId.isBlank()) {
                boolean isMember = conversationMemberRepository.existsByConversationIdAndUserId(conversationId, user.getName());
                if (!isMember) {
                    log.warn("User {} attempted unauthorized send to conversation {}", user.getName(), conversationId);
                    throw new AccessDeniedException("User is not a member of conversation " + conversationId);
                }
            }
        }
    }

    private String extractConversationId(String destination, String prefix) {
        if (destination == null || !destination.startsWith(prefix)) {
            return null;
        }
        String sub = destination.substring(prefix.length());
        int slashIndex = sub.indexOf('/');
        return slashIndex == -1 ? sub : sub.substring(0, slashIndex);
    }
}
