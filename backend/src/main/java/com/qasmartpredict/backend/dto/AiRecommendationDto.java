package com.qasmartpredict.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiRecommendationDto {

    private String riskLevel;

    private String summary;

    private List<String> recommendations;
	private List<String> strengths;

    private List<String> weaknesses;

    private String sprintDecision;
}