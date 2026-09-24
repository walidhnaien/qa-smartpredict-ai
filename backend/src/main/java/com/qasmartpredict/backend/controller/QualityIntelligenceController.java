package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.QualityIntelligenceDto;
import com.qasmartpredict.backend.service.QualityIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/quality-intelligence")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QualityIntelligenceController {

    private final QualityIntelligenceService service;

    /*
     * ==========================================================
     * QIS GLOBAL / HISTORIQUE
     * ==========================================================
     */
    @GetMapping
    public QualityIntelligenceDto getScore() {

        return service.calculate();
    }


    /*
     * ==========================================================
     * QIS METIER PAR RELEASE
     * ==========================================================
     */
    @GetMapping("/release/{releaseId}")
    public QualityIntelligenceDto getScoreByRelease(
            @PathVariable UUID releaseId) {

        return service.calculate(releaseId);
    }
}