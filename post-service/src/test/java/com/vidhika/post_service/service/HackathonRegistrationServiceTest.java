package com.vidhika.post_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidhika.post_service.dto.RegisterHackathonRequest;
import com.vidhika.post_service.enums.HackathonMode;
import com.vidhika.post_service.enums.HackathonStatus;
import com.vidhika.post_service.enums.OutboxStatus;
import com.vidhika.post_service.enums.RegistrationStatus;
import com.vidhika.post_service.model.Hackathon;
import com.vidhika.post_service.model.HackathonRegistration;
import com.vidhika.post_service.model.OutboxEvent;
import com.vidhika.post_service.repository.HackathonRegistrationRepository;
import com.vidhika.post_service.repository.HackathonRepository;
import com.vidhika.post_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HackathonRegistrationServiceTest {

    @Mock
    private HackathonRepository hackathonRepository;

    @Mock
    private HackathonRegistrationRepository registrationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private HackathonRegistrationService registrationService;

    private UUID hackathonId;
    private UUID user1Id;
    private UUID user2Id;
    private Hackathon hackathon;

    @BeforeEach
    void setUp() {
        hackathonId = UUID.randomUUID();
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();

        hackathon = Hackathon.builder()
                .id(hackathonId)
                .organizerId(UUID.randomUUID())
                .title("Pulse Hackathon 2026")
                .description("Build microservices")
                .mode(HackathonMode.ONLINE)
                .status(HackathonStatus.PUBLISHED)
                .startDate(LocalDateTime.now().plusDays(2))
                .endDate(LocalDateTime.now().plusDays(5))
                .registrationDeadline(LocalDateTime.now().plusDays(1))
                .minTeamSize(2)
                .maxTeamSize(4)
                .build();
    }

    @Test
    void registerForHackathon_SoloRegistration_DoesNotPublishTeamEvent() {
        when(hackathonRepository.findById(hackathonId)).thenReturn(Optional.of(hackathon));
        when(registrationRepository.existsByHackathonIdAndUserId(hackathonId, user1Id)).thenReturn(false);

        HackathonRegistration saved = HackathonRegistration.builder()
                .id(UUID.randomUUID())
                .hackathonId(hackathonId)
                .userId(user1Id)
                .teamName(null)
                .status(RegistrationStatus.REGISTERED)
                .registeredAt(LocalDateTime.now())
                .build();

        when(registrationRepository.save(any(HackathonRegistration.class))).thenReturn(saved);

        RegisterHackathonRequest request = new RegisterHackathonRequest();
        request.setTeamName(null);

        registrationService.registerForHackathon(hackathonId, user1Id, request);

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void registerForHackathon_FirstMemberOnly_DoesNotPublishIfBelowMinTeamSize() {
        when(hackathonRepository.findById(hackathonId)).thenReturn(Optional.of(hackathon));
        when(registrationRepository.existsByHackathonIdAndUserId(hackathonId, user1Id)).thenReturn(false);
        when(registrationRepository.countByHackathonIdAndTeamNameIgnoreCase(hackathonId, "AlphaCoders")).thenReturn(0L);

        HackathonRegistration member1 = HackathonRegistration.builder()
                .id(UUID.randomUUID())
                .hackathonId(hackathonId)
                .userId(user1Id)
                .teamName("AlphaCoders")
                .status(RegistrationStatus.REGISTERED)
                .registeredAt(LocalDateTime.now())
                .build();

        when(registrationRepository.save(any(HackathonRegistration.class))).thenReturn(member1);
        when(registrationRepository.findByHackathonIdAndTeamNameIgnoreCase(hackathonId, "AlphaCoders"))
                .thenReturn(List.of(member1));

        RegisterHackathonRequest request = new RegisterHackathonRequest();
        request.setTeamName("AlphaCoders");

        registrationService.registerForHackathon(hackathonId, user1Id, request);

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void registerForHackathon_SecondMemberRegisters_PublishesTeamFormedEvent() throws Exception {
        when(hackathonRepository.findById(hackathonId)).thenReturn(Optional.of(hackathon));
        when(registrationRepository.existsByHackathonIdAndUserId(hackathonId, user2Id)).thenReturn(false);
        when(registrationRepository.countByHackathonIdAndTeamNameIgnoreCase(hackathonId, "AlphaCoders")).thenReturn(1L);

        HackathonRegistration member1 = HackathonRegistration.builder()
                .id(UUID.randomUUID())
                .hackathonId(hackathonId)
                .userId(user1Id)
                .teamName("AlphaCoders")
                .status(RegistrationStatus.REGISTERED)
                .registeredAt(LocalDateTime.now().minusHours(1))
                .build();

        HackathonRegistration member2 = HackathonRegistration.builder()
                .id(UUID.randomUUID())
                .hackathonId(hackathonId)
                .userId(user2Id)
                .teamName("AlphaCoders")
                .status(RegistrationStatus.REGISTERED)
                .registeredAt(LocalDateTime.now())
                .build();

        when(registrationRepository.save(any(HackathonRegistration.class))).thenReturn(member2);
        when(registrationRepository.findByHackathonIdAndTeamNameIgnoreCase(hackathonId, "AlphaCoders"))
                .thenReturn(List.of(member1, member2));

        RegisterHackathonRequest request = new RegisterHackathonRequest();
        request.setTeamName("AlphaCoders");

        registrationService.registerForHackathon(hackathonId, user2Id, request);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(captor.capture());

        OutboxEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("TEAM_FORMED");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.isProcessed()).isFalse();

        UUID expectedTeamId = UUID.nameUUIDFromBytes(
                (hackathonId.toString() + ":alphacoders").getBytes(StandardCharsets.UTF_8)
        );
        assertThat(event.getAggregateId()).isEqualTo(expectedTeamId);

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.get("eventType").asText()).isEqualTo("TEAM_FORMED");
        assertThat(payload.get("teamId").asText()).isEqualTo(expectedTeamId.toString());
        assertThat(payload.get("hackathonId").asText()).isEqualTo(hackathonId.toString());
        assertThat(payload.get("teamName").asText()).isEqualTo("AlphaCoders");

        JsonNode members = payload.get("memberIds");
        assertThat(members.isArray()).isTrue();
        assertThat(members.size()).isEqualTo(2);
        assertThat(members.get(0).asText()).isEqualTo(user1Id.toString());
        assertThat(members.get(1).asText()).isEqualTo(user2Id.toString());
    }
}
