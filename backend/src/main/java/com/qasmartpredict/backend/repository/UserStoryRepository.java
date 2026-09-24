package com.qasmartpredict.backend.repository;

import com.qasmartpredict.backend.domain.UserStoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStoryRepository
        extends JpaRepository<UserStoryEntity, UUID> {

    // Recherche globale Jira
    Optional<UserStoryEntity> findByJiraKey(String jiraKey);

    // KPI historiques / globaux
    long countByIssueType(String issueType);

    long countByIssueTypeAndTsTicketIdIsNotNull(String issueType);

    // =====================================================
    // RELEASE MANAGEMENT
    // =====================================================

    List<UserStoryEntity> findDistinctByReleases_Id(UUID releaseId);

    long countDistinctByReleases_IdAndIssueType(
            UUID releaseId,
            String issueType
    );

    Optional<UserStoryEntity> findByReleases_IdAndJiraKey(
            UUID releaseId,
            String jiraKey
    );
	
	
	List<UserStoryEntity>
findDistinctByReleases_IdAndIssueType(
        UUID releaseId,
        String issueType
);

List<UserStoryEntity>
findByEpicKey(String epicKey);
	
	
}