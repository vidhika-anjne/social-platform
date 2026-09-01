package com.vidhika.post_service.repository;

import com.vidhika.post_service.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    Optional<Like> findByPostIdAndUserId(UUID postId, UUID userId);

    List<Like> findByPostIdOrderByCreatedAtDesc(UUID postId);

    long countByPostId(UUID postId);
}
