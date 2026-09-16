package com.qasmartpredict.backend.repository;

import com.qasmartpredict.backend.domain.RcaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RcaRepository extends JpaRepository<RcaEntity, Long> {

    Optional<RcaEntity> findByJiraId(String jiraId);
}