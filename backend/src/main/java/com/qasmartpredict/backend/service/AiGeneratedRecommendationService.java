package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qasmartpredict.backend.dto.AiRecommendationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiGeneratedRecommendationService {

    private final AiPromptBuilderService aiPromptBuilderService;
    private final OpenAiClientService openAiClientService;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    public AiRecommendationDto generateStructuredRecommendation() {

        try {

            String prompt =
                    aiPromptBuilderService.buildPrompt();

            String json =
                    openAiClientService.generate(prompt);

            return objectMapper.readValue(
                    json,
                    AiRecommendationDto.class
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to parse AI response",
                    e
            );
        }
    }
}