package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.AiRecommendationDto;
import com.qasmartpredict.backend.service.AiGeneratedRecommendationService;
import com.qasmartpredict.backend.service.AiPromptBuilderService;
import com.qasmartpredict.backend.service.OpenAiClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiPromptController {

    private final AiPromptBuilderService promptBuilderService;
    private final OpenAiClientService openAiClientService;
    private final AiGeneratedRecommendationService generatedRecommendationService;

    @GetMapping("/prompt")
    public String prompt() {
        return promptBuilderService.buildPrompt();
    }

    @GetMapping("/raw-generate")
    public String rawGenerate() {
        String prompt = promptBuilderService.buildPrompt();
        return openAiClientService.generate(prompt);
    }

    @GetMapping("/generate")
    public AiRecommendationDto generate() {
        return generatedRecommendationService.generateStructuredRecommendation();
    }
}