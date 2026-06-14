package com.qasmartpredict.backend.dto;

import lombok.Data;

@Data
public class QualityIntelligenceDto {

    private double coverageScore;

    private double defectScore;

    private double feedbackScore;

    private double incidentScore;

    private double qualityIntelligenceScore;
}