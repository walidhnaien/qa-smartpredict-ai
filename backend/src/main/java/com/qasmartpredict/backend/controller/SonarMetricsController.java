package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.SonarMetric;
import com.qasmartpredict.backend.repository.SonarMetricRepository;
import com.qasmartpredict.backend.service.QisSonarIntegrationService;
import com.qasmartpredict.backend.service.SonarImportService;
import com.qasmartpredict.backend.service.SonarScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.qasmartpredict.backend.dto.QisSonarResponse;

import java.util.List;

@RestController
@RequestMapping("/api/sonar")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class SonarMetricsController {

    private final SonarMetricRepository repository;
    private final SonarImportService sonarImportService;
    private final SonarScoreService sonarScoreService;
    private final QisSonarIntegrationService qisSonarIntegrationService;
	

    @PostMapping
    public ResponseEntity<SonarMetric> save(
            @RequestBody SonarMetric sonarMetric) {

        double score =
                sonarScoreService.calculateScore(sonarMetric);

        sonarMetric.setSonarScore(score);

        SonarMetric saved =
                repository.save(sonarMetric);

        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public List<SonarMetric> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SonarMetric> findById(
            @PathVariable Long id) {

        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/project/{projectId}/latest")
    public ResponseEntity<SonarMetric> latestForProject(
            @PathVariable Long projectId) {

        return repository
                .findTopByProjectIdOrderByAnalysisDateDesc(
                        projectId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
@PostMapping("/import-static")
public ResponseEntity<SonarMetric> importStatic(
        @RequestParam Long projectId,
        @RequestParam Long releaseId) {

    try {

        SonarMetric sonarMetric =
                sonarImportService.importStaticSonarResult(
                        projectId,
                        releaseId);

        return ResponseEntity.ok(sonarMetric);

    } catch (Exception e) {

        e.printStackTrace();

        return ResponseEntity
                .internalServerError()
                .build();
    }
}


@GetMapping("/qis/{projectId}")
public ResponseEntity<QisSonarResponse> calculateQisWithSonar(
        @PathVariable Long projectId,
        @RequestParam double currentQis) {

    QisSonarResponse result =
            qisSonarIntegrationService
                    .calculateFinalQis(
                            projectId,
                            currentQis);

    return ResponseEntity.ok(result);
}


























}