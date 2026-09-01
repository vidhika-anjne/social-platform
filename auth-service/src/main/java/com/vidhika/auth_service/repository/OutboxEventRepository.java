package com.vidhika.auth_service.repository;

import com.vidhika.auth_service.enums.OutboxStatus;
import com.vidhika.auth_service.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusInAndRetryCountLessThan(
            List<OutboxStatus> statuses,
            int maxRetryCount,
            Pageable pageable
    );
}
