package com.qasmartpredict.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class QualitySnapshotDto {

    private LocalDateTime analysisDate;

    private double qis;

    private double coverageScore;

    private double defectScore;

    private double feedbackScore;

    private double incidentScore;

    private double sonarScore;
}