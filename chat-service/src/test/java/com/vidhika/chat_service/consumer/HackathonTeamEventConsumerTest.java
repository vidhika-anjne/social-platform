package com.vidhika.chat_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.chat_service.service.ConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HackathonTeamEventConsumerTest {

    @Mock
    private ConversationService conversationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private HackathonTeamEventConsumer consumer;

    @Test
    void testConsumeTeamFormedEvent_WithWrapper() {
        String json = """
                {
                    "eventType": "TEAM_FORMED",
                    "payload": {
                        "teamId": "t-100",
                        "hackathonId": "h-200",
                        "memberIds": ["u1", "u2", "u3"],
                        "teamName": "AI Hackers"
                    }
                }
                """;

        consumer.consumeTeamFormedEvent(json);

        verify(conversationService).createHackathonTeamChat(
                eq("t-100"),
                eq("h-200"),
                eq(List.of("u1", "u2", "u3")),
                eq("AI Hackers")
        );
    }

    @Test
    void testConsumeTeamFormedEvent_DirectPayload() {
        String json = """
                {
                    "teamId": "team-xyz",
                    "hackathonId": "hack-abc",
                    "memberIds": ["user-1", "user-2"]
                }
                """;

        consumer.consumeTeamFormedEvent(json);

        verify(conversationService).createHackathonTeamChat(
                eq("team-xyz"),
                eq("hack-abc"),
                eq(List.of("user-1", "user-2")),
                eq(null)
        );
    }
}
