package com.qasmartpredict.backend.dto;

import com.qasmartpredict.backend.domain.ReleaseStatus;
import com.qasmartpredict.backend.domain.ReleaseType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReleaseDto(

        UUID id,

        UUID applicationId,

        String name,

        String version,

        ReleaseType type,

        ReleaseStatus status,

        LocalDate startDate,

        LocalDate endDate,

        LocalDateTime createdAt

) {
}