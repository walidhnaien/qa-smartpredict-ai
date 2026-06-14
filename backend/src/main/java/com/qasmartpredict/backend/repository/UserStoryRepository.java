package com.qasmartpredict.backend.repository;

import com.qasmartpredict.backend.domain.UserStoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserStoryRepository extends JpaRepository<UserStoryEntity, UUID> {
    Optional<UserStoryEntity> findByJiraKey(String jiraKey);
	long countByIssueType(String issueType);
	long countByIssueTypeAndTsTicketIdIsNotNull(String issueType);
}