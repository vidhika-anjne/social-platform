package com.vidhika.user_service.repository;

import com.vidhika.user_service.model.GithubContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
