package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.QisSonarResponse;
import com.qasmartpredict.backend.service.ReleaseAnalysisService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ReleaseAnalysisController {

    private final ReleaseAnalysisService
            releaseAnalysisService;


    @PostMapping("/{releaseId}/analyze")
    public QisSonarResponse analyzeRelease(
            @PathVariable UUID releaseId) {

        return releaseAnalysisService
                .analyzeRelease(releaseId);
    }
}