package com.qasmartpredict.backend.dto;

import java.util.UUID;

public record ReleaseQualityDto(

        UUID releaseId,
        String releaseVersion,

        double coverageScore,

        double defectScore,

        double feedbackScore,

        double incidentScore,

        int totalRequirements,
        int coveredRequirements,

        long totalStories,
        long totalBugs,

        long totalIncidents,
        long criticalClientBugs

) {
}