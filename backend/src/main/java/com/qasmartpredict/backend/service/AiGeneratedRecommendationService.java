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
        try {
            String prompt = aiPromptBuilderService.buildPrompt(metrics);
            String json = openAiClientService.generate(prompt);
            return objectMapper.readValue(json, AiRecommendationDto.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate or parse AI recommendation", e);
        }
    }
}
