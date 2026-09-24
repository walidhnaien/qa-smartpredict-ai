package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.ApplicationEntity;
import com.qasmartpredict.backend.domain.ReleaseEntity;
import com.qasmartpredict.backend.domain.ReleaseStatus;
import com.qasmartpredict.backend.dto.CreateReleaseRequest;
import com.qasmartpredict.backend.dto.ReleaseDto;
import com.qasmartpredict.backend.repository.ApplicationRepository;
import com.qasmartpredict.backend.repository.ReleaseRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional
    public ReleaseDto create(CreateReleaseRequest request) {

        if (request.version() == null
                || request.version().isBlank()) {

            throw new IllegalArgumentException(
                    "Release version is required"
            );
        }

        if (request.name() == null
                || request.name().isBlank()) {

            throw new IllegalArgumentException(
                    "Release name is required"
            );
        }

        if (request.type() == null) {

            throw new IllegalArgumentException(
                    "Release type is required"
            );
        }

        if (request.applicationId() == null) {

            throw new IllegalArgumentException(
                    "Application id is required"
            );
        }

        String version = request.version().trim();

        if (releaseRepository.existsByVersion(version)) {

            throw new IllegalArgumentException(
                    "Release already exists: " + version
            );
        }

        ApplicationEntity application =
                applicationRepository
                        .findById(request.applicationId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Application not found: "
                                                + request.applicationId()
                                )
                        );

        ReleaseEntity entity =
                ReleaseEntity.builder()
                        .application(application)
                        .name(request.name().trim())
                        .version(version)
                        .type(request.type())
                        .status(ReleaseStatus.DRAFT)
                        .startDate(request.startDate())
                        .endDate(request.endDate())
                        .createdAt(LocalDateTime.now())
                        .build();

        ReleaseEntity saved =
                releaseRepository.save(entity);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ReleaseDto> findAll() {

        return releaseRepository
                .findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReleaseDto findById(UUID id) {

        ReleaseEntity entity =
                releaseRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Release not found: " + id
                                )
                        );

        return toDto(entity);
    }

    private ReleaseDto toDto(ReleaseEntity entity) {

        return new ReleaseDto(
                entity.getId(),
                entity.getApplication().getId(),
                entity.getName(),
                entity.getVersion(),
                entity.getType(),
                entity.getStatus(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCreatedAt()
        );
    }
}