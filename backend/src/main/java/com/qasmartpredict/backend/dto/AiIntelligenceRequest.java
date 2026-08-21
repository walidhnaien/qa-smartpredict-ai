package com.qasmartpredict.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiIntelligenceRequest {

    private double qis;

    private double coverageScore;
    private double defectScore;
    private double feedbackScore;
    private double incidentScore;

    private double sonarScore;

    private Integer bugs;
    private Integer vulnerabilities;
    private Integer codeSmells;

    private Double sonarCoverage;
    private Double duplication;

    private Integer totalRequirements;
    private Integer coveredRequirements;
    private Integer uncoveredRequirements;

    private Long totalStories;
    private Long totalBugs;
    private Double bugRatio;
}