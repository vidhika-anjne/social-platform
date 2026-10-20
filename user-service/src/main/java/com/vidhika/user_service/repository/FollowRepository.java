package com.vidhika.user_service.repository;

import com.vidhika.user_service.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    // Check if a follow relationship already exists
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    // Find a specific follow relationship (for deletion)
    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    // Get all followers of a user (people who follow targetUserId)
    List<Follow> findByFollowingId(UUID followingId);

    // Get all users a user is following
    List<Follow> findByFollowerId(UUID followerId);

    // Count followers of a user
    long countByFollowingId(UUID followingId);

    // Count how many users a user is following
    long countByFollowerId(UUID followerId);

    // Get all users a user is following and Sorting is done in ascending order on the basis of ID.
    @Query(nativeQuery = true,value = """
        SELECT * 
        FROM follows 
        WHERE follower_id = :followerId
        ORDER BY id ASC
    """)
    List<Follow> findByFollowerIdOrderByIdAsc(@Param("followerId") UUID followerId);

    // Get all the followers of list of users
    List<Follow> findByFollowerIdIn(List<UUID> followerIds);
    List<Follow> findByFollowerIdInOrderByIdAsc(List<UUID> followerId);
    List<Follow> findByFollowerIdInOrderByFollowingIdAsc(List<UUID> followerId);
}
