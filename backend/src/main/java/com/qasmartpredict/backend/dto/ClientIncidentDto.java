package com.qasmartpredict.backend.dto;

public record ClientIncidentDto(
        String client,
        long incidentCount,
        long criticalCount
) {}