package com.qasmartpredict.backend.repository;

import com.qasmartpredict.backend.domain.ReleaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReleaseRepository extends JpaRepository<ReleaseEntity, UUID> {
	
	Optional <ReleaseEntity> findByVersion(String version);
}