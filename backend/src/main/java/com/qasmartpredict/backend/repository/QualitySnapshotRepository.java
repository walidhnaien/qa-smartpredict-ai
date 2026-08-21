package com.qasmartpredict.backend.repository;

import com.qasmartpredict.backend.domain.QualitySnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QualitySnapshotRepository
        extends JpaRepository<QualitySnapshotEntity, UUID> {

    List<QualitySnapshotEntity>
        findTop20ByOrderByAnalysisDateDesc();
}