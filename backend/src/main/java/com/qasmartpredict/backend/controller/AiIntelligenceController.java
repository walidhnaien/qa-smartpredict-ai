package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.AiIntelligenceRequest;
import com.qasmartpredict.backend.dto.AiIntelligenceResponse;
import com.qasmartpredict.backend.service.AiIntelligenceService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AiIntelligenceController {

    private final AiIntelligenceService aiIntelligenceService;


    @PostMapping("/analyze")
    public AiIntelligenceResponse analyze(
            @RequestBody AiIntelligenceRequest request) {

        return aiIntelligenceService.analyze(request);
    }
}