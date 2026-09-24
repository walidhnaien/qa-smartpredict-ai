package com.qasmartpredict.backend.dto;

import com.qasmartpredict.backend.domain.ReleaseType;

import java.time.LocalDate;
import java.util.UUID;

public record CreateReleaseRequest(

        UUID applicationId,

        String name,

        String version,

        ReleaseType type,

        LocalDate startDate,

        LocalDate endDate

) {
}