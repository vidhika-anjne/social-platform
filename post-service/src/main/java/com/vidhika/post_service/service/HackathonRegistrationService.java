package com.vidhika.post_service.service;

import com.vidhika.post_service.dto.HackathonRegistrationResponse;
import com.vidhika.post_service.dto.RegisterHackathonRequest;
import com.vidhika.post_service.dto.UpdateRegistrationStatusRequest;
import com.vidhika.post_service.enums.HackathonStatus;
import com.vidhika.post_service.enums.RegistrationStatus;
import com.vidhika.post_service.exception.HackathonNotFoundException;
import com.vidhika.post_service.exception.RegistrationNotFoundException;
import com.vidhika.post_service.exception.UnauthorizedActionException;
import com.vidhika.post_service.model.Hackathon;
import com.vidhika.post_service.model.HackathonRegistration;
import com.vidhika.post_service.repository.HackathonRegistrationRepository;
import com.vidhika.post_service.repository.HackathonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HackathonRegistrationService {

    private final HackathonRepository hackathonRepository;
    private final HackathonRegistrationRepository registrationRepository;
    
    @Transactional
    public HackathonRegistrationResponse registerForHackathon(UUID hackathonId, UUID userId, RegisterHackathonRequest request) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new HackathonNotFoundException("Hackathon not found with id: " + hackathonId));

        if (hackathon.getStatus() == HackathonStatus.DRAFT || hackathon.getStatus() == HackathonStatus.ENDED) {
            throw new IllegalStateException("Registrations are closed for this hackathon");
        }

        if (LocalDateTime.now().isAfter(hackathon.getRegistrationDeadline())) {
            throw new IllegalStateException("Registration deadline has passed on " + hackathon.getRegistrationDeadline());
        }

        if (registrationRepository.existsByHackathonIdAndUserId(hackathonId, userId)) {
            throw new IllegalStateException("You are already registered for this hackathon");
        }

        String teamName = (request != null && request.getTeamName() != null && !request.getTeamName().trim().isEmpty())
                ? request.getTeamName().trim()
                : null;

        // If it's a solo-only hackathon (maxTeamSize == 1), prevent team registration
        if (hackathon.getMaxTeamSize() != null && hackathon.getMaxTeamSize() == 1) {
            if (teamName != null) {
                throw new IllegalArgumentException("This hackathon is for solo participants only (maximum team size is 1)");
            }
        }

        // If teamName is provided, verify current team count against maxTeamSize
        if (teamName != null) {
            long currentMembers = registrationRepository.countByHackathonIdAndTeamNameIgnoreCase(hackathonId, teamName);
            if (hackathon.getMaxTeamSize() != null && currentMembers >= hackathon.getMaxTeamSize()) {
                throw new IllegalStateException("Team '" + teamName + "' is already full. Maximum allowed team size is " + hackathon.getMaxTeamSize());
            }
        }

        HackathonRegistration registration = HackathonRegistration.builder()
                .hackathonId(hackathonId)
                .userId(userId)
                .teamName(teamName)
                .status(RegistrationStatus.REGISTERED)
                .build();

        HackathonRegistration saved = registrationRepository.save(registration);
        log.info("User {} registered for hackathon {} (team: {}) with registrationId {}",
                userId, hackathonId, teamName, saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<HackathonRegistrationResponse> getTeamMembers(UUID hackathonId, String teamName) {
        if (!hackathonRepository.existsById(hackathonId)) {
            throw new HackathonNotFoundException("Hackathon not found with id: " + hackathonId);
        }
        return registrationRepository.findByHackathonIdAndTeamNameIgnoreCase(hackathonId, teamName.trim())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HackathonRegistrationResponse> getRegistrationsForHackathon(UUID hackathonId, UUID organizerId, String role) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new HackathonNotFoundException("Hackathon not found with id: " + hackathonId));

        if (!hackathon.getOrganizerId().equals(organizerId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new UnauthorizedActionException("You are not authorized to view registrations for this hackathon");
        }

        return registrationRepository.findByHackathonId(hackathonId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HackathonRegistrationResponse> getMyRegistrations(UUID userId) {
        return registrationRepository.findByUserIdOrderByRegisteredAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public HackathonRegistrationResponse updateRegistrationStatus(
            UUID hackathonId,
            UUID registrationId,
            UUID organizerId,
            String role,
            UpdateRegistrationStatusRequest request) {

        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new HackathonNotFoundException("Hackathon not found with id: " + hackathonId));

        if (!hackathon.getOrganizerId().equals(organizerId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new UnauthorizedActionException("You are not authorized to update registration status for this hackathon");
        }

        HackathonRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new RegistrationNotFoundException("Registration not found with id: " + registrationId));

        if (!registration.getHackathonId().equals(hackathonId)) {
            throw new IllegalArgumentException("Registration does not belong to hackathon " + hackathonId);
        }

        registration.setStatus(request.getStatus());
        HackathonRegistration updated = registrationRepository.save(registration);
        log.info("Registration {} status updated to {} by organizer {}", registrationId, request.getStatus(), organizerId);
        return toResponse(updated);
    }

    private HackathonRegistrationResponse toResponse(HackathonRegistration r) {
        return HackathonRegistrationResponse.builder()
                .id(r.getId())
                .hackathonId(r.getHackathonId())
                .userId(r.getUserId())
                .teamName(r.getTeamName())
                .status(r.getStatus())
                .registeredAt(r.getRegisteredAt())
                .build();
    }
}
