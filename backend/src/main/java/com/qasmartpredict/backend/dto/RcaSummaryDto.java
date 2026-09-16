package com.qasmartpredict.backend.dto;

public record RcaSummaryDto(

        long totalIncidents,

        long incidentsWithRca,

        long done,

        long inProgress,

        long toDo,

        double rcaCoverage,

        double correctiveActionCoverage,

        double preventiveActionCoverage,

        double closureRate,

        double rcaScore
) {}