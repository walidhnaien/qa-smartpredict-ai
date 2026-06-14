package com.qasmartpredict.backend.dto;

import lombok.Data;

@Data
public class DefectSummaryDto {

    private long totalStories;

    private long totalBugs;

    private double bugRatio;

    private double defectScore;
}