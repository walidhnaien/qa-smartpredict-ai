package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.AiRecommendationDto;
import com.qasmartpredict.backend.service.AiGeneratedRecommendationService;
import com.qasmartpredict.backend.service.AiPromptBuilderService;
import com.qasmartpredict.backend.service.LocalAiClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiPromptController {

    private final AiPromptBuilderService promptBuilderService;
    private final LocalAiClientService localAiClientService;
    private final AiGeneratedRecommendationService generatedRecommendationService;

    @GetMapping("/prompt")
    public String prompt() {
        return promptBuilderService.buildPrompt();
    }

    @GetMapping("/raw-generate")
    public String rawGenerate() {
        return localAiClientService.generate(promptBuilderService.buildPrompt());
    }

    @PostMapping("/generate")
    public AiRecommendationDto generate(@RequestBody Map<String, Double> metrics) {
        return generatedRecommendationService.generateStructuredRecommendation(metrics);
    }
}
