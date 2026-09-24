package com.qasmartpredict.backend.repository;

import com.qasmartpredict.backend.domain.ReleaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReleaseRepository
        extends JpaRepository<ReleaseEntity, UUID> {

    Optional<ReleaseEntity> findByVersion(String version);

    List<ReleaseEntity> findByApplicationId(UUID applicationId);

    Optional<ReleaseEntity>
        findByApplicationIdAndVersion(
            UUID applicationId,
            String version
        );

    boolean existsByApplicationIdAndVersion(
        UUID applicationId,
        String version
    );

  

    boolean existsByVersion(String version);
}