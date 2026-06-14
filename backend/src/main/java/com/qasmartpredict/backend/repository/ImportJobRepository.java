package com.qasmartpredict.backend.repository;

import com.qasmartpredict.backend.domain.ImportJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, UUID> {
}