package com.vidhika.post_service.service;

import com.vidhika.post_service.dto.CreateHackathonRequest;
import com.vidhika.post_service.dto.HackathonResponse;
import com.vidhika.post_service.dto.UpdateHackathonRequest;
import com.vidhika.post_service.enums.HackathonStatus;
import com.vidhika.post_service.exception.HackathonNotFoundException;
import com.vidhika.post_service.exception.UnauthorizedActionException;
import com.vidhika.post_service.model.Hackathon;
import com.vidhika.post_service.repository.HackathonRegistrationRepository;
import com.vidhika.post_service.repository.HackathonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HackathonService {

    private final HackathonRepository hackathonRepository;
    private final HackathonRegistrationRepository registrationRepository;

    @Transactional
    public HackathonResponse createHackathon(UUID organizerId, String role, CreateHackathonRequest request) {
        if (!isOrganizerOrAdmin(role)) {
            log.warn("Non-organizer user {} attempted to create a hackathon", organizerId);
            throw new UnauthorizedActionException("Only organizers can create hackathons");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        if (request.getRegistrationDeadline().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Registration deadline cannot be after end date");
        }

        Hackathon hackathon = Hackathon.builder()
                .organizerId(organizerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .bannerUrl(request.getBannerUrl())
                .logoUrl(request.getLogoUrl())
                .mode(request.getMode())
                .venue(request.getVenue())
                .websiteUrl(request.getWebsiteUrl())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .registrationDeadline(request.getRegistrationDeadline())
                .minTeamSize(request.getMinTeamSize() != null ? request.getMinTeamSize() : 1)
                .maxTeamSize(request.getMaxTeamSize() != null ? request.getMaxTeamSize() : 4)
                .status(request.getStatus() != null ? request.getStatus() : HackathonStatus.REGISTRATION_OPEN)
                .build();

        Hackathon saved = hackathonRepository.save(hackathon);
        log.info("Hackathon created with id: {} by organizer: {}", saved.getId(), organizerId);
        return toResponse(saved, 0L);
    }

    @Transactional(readOnly = true)
    public HackathonResponse getHackathon(UUID hackathonId) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new HackathonNotFoundException("Hackathon not found with id: " + hackathonId));
        long count = registrationRepository.countByHackathonId(hackathonId);
        return toResponse(hackathon, count);
    }

    @Transactional(readOnly = true)
    public List<HackathonResponse> getAllHackathons(HackathonStatus status) {
        List<Hackathon> list = (status != null)
                ? hackathonRepository.findByStatusOrderByStartDateAsc(status)
                : hackathonRepository.findAllByOrderByCreatedAtDesc();

        return list.stream()
                .map(h -> toResponse(h, registrationRepository.countByHackathonId(h.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HackathonResponse> getMyHackathons(UUID organizerId) {
        return hackathonRepository.findByOrganizerIdOrderByCreatedAtDesc(organizerId)
                .stream()
                .map(h -> toResponse(h, registrationRepository.countByHackathonId(h.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public HackathonResponse updateHackathon(UUID hackathonId, UUID organizerId, String role, UpdateHackathonRequest request) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new HackathonNotFoundException("Hackathon not found with id: " + hackathonId));

        if (!hackathon.getOrganizerId().equals(organizerId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new UnauthorizedActionException("You are not authorized to update this hackathon");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            hackathon.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            hackathon.setDescription(request.getDescription());
        }
        if (request.getBannerUrl() != null) {
            hackathon.setBannerUrl(request.getBannerUrl());
        }
        if (request.getLogoUrl() != null) {
            hackathon.setLogoUrl(request.getLogoUrl());
        }
        if (request.getMode() != null) {
            hackathon.setMode(request.getMode());
        }
        if (request.getVenue() != null) {
            hackathon.setVenue(request.getVenue());
        }
        if (request.getWebsiteUrl() != null) {
            hackathon.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getStartDate() != null) {
            hackathon.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            hackathon.setEndDate(request.getEndDate());
        }
        if (request.getRegistrationDeadline() != null) {
            hackathon.setRegistrationDeadline(request.getRegistrationDeadline());
        }
        if (request.getMinTeamSize() != null) {
            hackathon.setMinTeamSize(request.getMinTeamSize());
        }
        if (request.getMaxTeamSize() != null) {
            hackathon.setMaxTeamSize(request.getMaxTeamSize());
        }
        if (request.getStatus() != null) {
            hackathon.setStatus(request.getStatus());
        }

        Hackathon updated = hackathonRepository.save(hackathon);
        long count = registrationRepository.countByHackathonId(hackathonId);
        log.info("Hackathon {} updated by organizer {}", hackathonId, organizerId);
        return toResponse(updated, count);
    }

    @Transactional
    public void deleteHackathon(UUID hackathonId, UUID organizerId, String role) {
        Hackathon hackathon = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new HackathonNotFoundException("Hackathon not found with id: " + hackathonId));

        if (!hackathon.getOrganizerId().equals(organizerId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new UnauthorizedActionException("You are not authorized to delete this hackathon");
        }

        hackathonRepository.delete(hackathon);
        log.info("Hackathon {} deleted by organizer {}", hackathonId, organizerId);
    }

    public boolean existsById(UUID hackathonId) {
        return hackathonRepository.existsById(hackathonId);
    }

    private boolean isOrganizerOrAdmin(String role) {
        return role != null && ("ORGANIZER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role));
    }

    private HackathonResponse toResponse(Hackathon h, Long count) {
        return HackathonResponse.builder()
                .id(h.getId())
                .organizerId(h.getOrganizerId())
                .title(h.getTitle())
                .description(h.getDescription())
                .bannerUrl(h.getBannerUrl())
                .logoUrl(h.getLogoUrl())
                .mode(h.getMode())
                .venue(h.getVenue())
                .websiteUrl(h.getWebsiteUrl())
                .startDate(h.getStartDate())
                .endDate(h.getEndDate())
                .registrationDeadline(h.getRegistrationDeadline())
                .minTeamSize(h.getMinTeamSize())
                .maxTeamSize(h.getMaxTeamSize())
                .status(h.getStatus())
                .registrationCount(count)
                .createdAt(h.getCreatedAt())
                .updatedAt(h.getUpdatedAt())
                .build();
    }
}
