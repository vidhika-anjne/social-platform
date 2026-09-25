package com.vidhika.feed_service.repository;

import com.vidhika.feed_service.model.FeedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, UUID> {

    List<FeedItem> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
