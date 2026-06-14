package com.qasmartpredict.backend.repository;

import com.qasmartpredict.backend.domain.RequirementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RequirementRepository extends JpaRepository<RequirementEntity, UUID> {
    Optional<RequirementEntity> findByCode(String code);
}