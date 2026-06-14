package com.qasmartpredict.backend.dto;

import lombok.Data;

@Data
public class CoverageSummaryDto {

    private int totalRequirements;

    private int coveredRequirements;

    private double coveragePercentage;
}