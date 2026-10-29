package com.vidhika.chat_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.chat_service.dto.request.AddMemberRequest;
import com.vidhika.chat_service.dto.request.CreateConversationRequest;
import com.vidhika.chat_service.dto.request.SendMessageRequest;
import com.vidhika.chat_service.enums.ConversationType;
import com.vidhika.chat_service.enums.MessageStatus;
import com.vidhika.chat_service.model.Conversation;
import com.vidhika.chat_service.model.Message;
import com.vidhika.chat_service.repository.ConversationMemberRepository;
import com.vidhika.chat_service.repository.ConversationRepository;
import com.vidhika.chat_service.repository.MessageRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ChatApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMemberRepository conversationMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String user1Id;
    private String user2Id;
    private String user3Id;

    private String tokenUser1;
    private String tokenUser2;
    private String tokenUser3;

    @BeforeEach
    void setUp() {
        user1Id = UUID.randomUUID().toString();
        user2Id = UUID.randomUUID().toString();
        user3Id = UUID.randomUUID().toString();

        tokenUser1 = generateTestToken(user1Id, "user1@pulse.com", "USER");
        tokenUser2 = generateTestToken(user2Id, "user2@pulse.com", "USER");
        tokenUser3 = generateTestToken(user3Id, "user3@pulse.com", "USER");
    }

    @AfterEach
    void tearDown() {
        messageRepository.deleteAll();
        conversationMemberRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    private String generateTestToken(String subject, String email, String role) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .subject(subject)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    void testCreateAndGetOneToOneConversation() throws Exception {
        // 1. Create 1-to-1 conversation
        CreateConversationRequest request = CreateConversationRequest.builder()
                .type(ConversationType.ONE_TO_ONE)
                .recipientId(user2Id)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.type").value("ONE_TO_ONE"))
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andReturn();

        String conversationId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asText();

        // 2. Creating duplicate 1-to-1 conversation should return existing conversation with same ID
        mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(conversationId));

        // 3. User 2 fetching conversation by ID (should succeed)
        mockMvc.perform(get("/api/v1/chats/" + conversationId)
                        .header("Authorization", "Bearer " + tokenUser2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId));

        // 4. User 3 (non-member) fetching conversation by ID should return 403 Forbidden
        mockMvc.perform(get("/api/v1/chats/" + conversationId)
                        .header("Authorization", "Bearer " + tokenUser3))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGroupConversationLifecycle() throws Exception {
        // 1. Create GROUP conversation
        CreateConversationRequest request = CreateConversationRequest.builder()
                .type(ConversationType.GROUP)
                .name("Hackathon Sprint")
                .memberIds(List.of(user2Id))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/chats")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Hackathon Sprint"))
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andReturn();

        String conversationId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asText();

        // 2. Add User 3 to the group
        AddMemberRequest addReq = AddMemberRequest.builder()
                .userId(user3Id)
                .build();

        mockMvc.perform(post("/api/v1/chats/" + conversationId + "/members")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // 3. Get Members list
        mockMvc.perform(get("/api/v1/chats/" + conversationId + "/members")
                        .header("Authorization", "Bearer " + tokenUser3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // 4. Remove User 2 from the group
        mockMvc.perform(delete("/api/v1/chats/" + conversationId + "/members/" + user2Id)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isNoContent());

        // Verify members is now 2
        mockMvc.perform(get("/api/v1/chats/" + conversationId + "/members")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 5. Delete conversation by creator
        mockMvc.perform(delete("/api/v1/chats/" + conversationId)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isNoContent());

        // Verify conversation is gone
        mockMvc.perform(get("/api/v1/chats/" + conversationId)
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isNotFound());
    }

    @Test
    void testMessageHistoryPagination() throws Exception {
        // Setup conversation
        Conversation conversation = conversationRepository.save(Conversation.builder()
                .type(ConversationType.GROUP)
                .name("Dev Chat")
                .createdBy(user1Id)
                .createdAt(LocalDateTime.now())
                .build());

        conversationMemberRepository.saveAll(List.of(
                com.vidhika.chat_service.model.ConversationMember.builder()
                        .conversationId(conversation.getId())
                        .userId(user1Id)
                        .build(),
                com.vidhika.chat_service.model.ConversationMember.builder()
                        .conversationId(conversation.getId())
                        .userId(user2Id)
                        .build()
        ));

        // Insert 15 test messages
        for (int i = 1; i <= 15; i++) {
            messageRepository.save(Message.builder()
                    .conversationId(conversation.getId())
                    .senderId(i % 2 == 0 ? user2Id : user1Id)
                    .content("Test message " + i)
                    .status(MessageStatus.SENT)
                    .createdAt(LocalDateTime.now().minusMinutes(15 - i))
                    .build());
        }

        // 1. Get first page of messages (size = 10)
        mockMvc.perform(get("/api/v1/chats/" + conversation.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .param("page", "0")
                        .param("size", "10")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(2));

        // 2. Get second page of messages (size = 10)
        mockMvc.perform(get("/api/v1/chats/" + conversation.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenUser2)
                        .param("page", "1")
                        .param("size", "10")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)));

        // 3. User 3 (non-member) cannot access message history
        mockMvc.perform(get("/api/v1/chats/" + conversation.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenUser3))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/chats"))
                .andExpect(status().isUnauthorized());
    }
}
