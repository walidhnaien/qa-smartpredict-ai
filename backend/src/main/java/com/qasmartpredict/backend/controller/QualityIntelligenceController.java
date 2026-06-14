package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.QualityIntelligenceDto;
import com.qasmartpredict.backend.service.QualityIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quality-intelligence")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QualityIntelligenceController {

    private final QualityIntelligenceService service;

    @GetMapping
    public QualityIntelligenceDto getScore() {

        return service.calculate();
    }
}