package com.vidhika.post_service.repository;

import com.vidhika.post_service.model.HackathonRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HackathonRegistrationRepository extends JpaRepository<HackathonRegistration, UUID> {

    List<HackathonRegistration> findByHackathonId(UUID hackathonId);

    List<HackathonRegistration> findByUserIdOrderByRegisteredAtDesc(UUID userId);

    Optional<HackathonRegistration> findByHackathonIdAndUserId(UUID hackathonId, UUID userId);

    boolean existsByHackathonIdAndUserId(UUID hackathonId, UUID userId);

    long countByHackathonId(UUID hackathonId);

    long countByHackathonIdAndTeamNameIgnoreCase(UUID hackathonId, String teamName);

    List<HackathonRegistration> findByHackathonIdAndTeamNameIgnoreCase(UUID hackathonId, String teamName);
}
