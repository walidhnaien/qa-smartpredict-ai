package com.qasmartpredict.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiIntelligenceResponse {

    private double qis;

    private String riskLevel;

    private String releaseDecision;

    private int confidence;

    private String mainRisk;

    private String summary;

    private String executiveSummary;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> recommendations;

    private AiForecastDto forecast;
	private AiGenerativeAnalysisDto aiAnalysis;
	
}