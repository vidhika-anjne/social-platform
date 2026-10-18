package com.vidhika.post_service.repository;

import com.vidhika.post_service.enums.HackathonStatus;
import com.vidhika.post_service.model.Hackathon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HackathonRepository extends JpaRepository<Hackathon, UUID> {

    List<Hackathon> findByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);

    List<Hackathon> findByStatusOrderByStartDateAsc(HackathonStatus status);

    List<Hackathon> findAllByOrderByCreatedAtDesc();
}