package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.AiRecommendationDto;
import com.qasmartpredict.backend.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiRecommendationController {

    private final AiRecommendationService service;

    @GetMapping("/recommendations")
    public AiRecommendationDto getRecommendations() {

        return service.generateRecommendations();
    }
}