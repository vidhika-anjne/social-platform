package com.vidhika.user_service.repository;

import com.vidhika.user_service.model.GithubContribution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubContributionRepository extends JpaRepository<GithubContribution, UUID> {

    Optional<GithubContribution> findByUserIdAndGithubRepositoryId(UUID userId, String githubRepositoryId);

    Optional<GithubContribution> findByUserIdAndRepositoryOwnerIgnoreCaseAndRepositoryNameIgnoreCase(
            UUID userId, String repositoryOwner, String repositoryName);

    List<GithubContribution> findByUserIdAndSelectedTrueOrderByCreatedAtDesc(UUID userId);

    List<GithubContribution> findByUserId(UUID userId);

    void deleteByUserIdAndRepositoryOwnerIgnoreCaseAndRepositoryNameIgnoreCase(
            UUID userId, String repositoryOwner, String repositoryName);

    @Query("SELECT c FROM GithubContribution c WHERE c.selected = true AND (c.nextSyncAt IS NULL OR c.nextSyncAt <= :now) ORDER BY c.nextSyncAt ASC")
    List<GithubContribution> findDueContributions(@Param("now") LocalDateTime now, Pageable pageable);
}
