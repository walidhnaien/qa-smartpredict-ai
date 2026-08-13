package com.qasmartpredict.backend.repository;

import com.qasmartpredict.backend.dto.SonarMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SonarMetricRepository
        extends JpaRepository<SonarMetric, Long> {

    Optional<SonarMetric>
            findTopByProjectIdOrderByAnalysisDateDesc(Long projectId);

    Optional<SonarMetric>
            findTopByReleaseIdOrderByAnalysisDateDesc(Long releaseId);
}