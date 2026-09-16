package com.qasmartpredict.backend.dto;

import lombok.Data;

@Data
public class QualityIntelligenceDto {

    
    private long incidentsWithRca;

private double rcaCoverage;

private double correctiveActionCoverage;

private double preventiveActionCoverage;

private double closureRate;

    private double coverageScore;

    private double defectScore;

    private double feedbackScore;

    private double incidentScore;

    private double rcaScore;

    private double qualityIntelligenceScore;

    // Coverage
    private int totalRequirements;

    private int coveredRequirements;

    private int uncoveredRequirements;

    // Defects
    private long totalStories;

    private long totalBugs;

    private double bugRatio;

    // Incidents clients
    private long totalIncidents;

    private long criticalClientBugs;

    private String mostImpactedClient;

    // RCA
    private long rcaDone;

    private long rcaInProgress;

    private long rcaToDo;
}