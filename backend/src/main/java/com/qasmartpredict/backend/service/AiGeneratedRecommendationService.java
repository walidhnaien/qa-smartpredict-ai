package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qasmartpredict.backend.dto.AiRecommendationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiGeneratedRecommendationService {

    private final AiPromptBuilderService aiPromptBuilderService;
    private final OpenAiClientService openAiClientService;
    private final ObjectMapper objectMapper;

    public AiRecommendationDto generateStructuredRecommendation(Map<String, Double> metrics) {
        double qis = AiPromptBuilderService.requireMetric(metrics, "qis");
        double coverage = AiPromptBuilderService.requireMetric(metrics, "coverageScore");
        double defect = AiPromptBuilderService.requireMetric(metrics, "defectScore");
        AiPromptBuilderService.requireMetric(metrics, "feedbackScore");
        AiPromptBuilderService.requireMetric(metrics, "incidentScore");

        String prompt = aiPromptBuilderService.buildPrompt(metrics);
        String json = openAiClientService.generate(prompt);
        try {
            AiRecommendationDto result = objectMapper.readValue(json, AiRecommendationDto.class);
            // Never allow generated text to override deterministic fields.
            result.setSprintDecision(AiPromptBuilderService.decisionFor(qis));
            result.setRiskLevel(AiPromptBuilderService.riskFor(coverage, defect));
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse AI recommendation JSON", e);
        }
    }
}
