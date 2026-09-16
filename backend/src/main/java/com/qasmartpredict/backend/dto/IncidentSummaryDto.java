package com.qasmartpredict.backend.dto;

import java.util.List;

public record IncidentSummaryDto(
    long totalBugs,
        long internalBugs,
        long totalIncidents,
        long criticalClientBugs,
        double incidentRate,
        double incidentScore,
        double feedbackScore,
        String mostImpactedClient,
        List<ClientIncidentDto> clients
) {}