package com.qasmartpredict.backend.repository;

import com.qasmartpredict.backend.domain.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {
}