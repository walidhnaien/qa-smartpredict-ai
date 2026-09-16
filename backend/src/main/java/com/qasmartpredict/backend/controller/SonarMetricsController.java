package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.QisSonarResponse;
import com.qasmartpredict.backend.dto.SonarMetric;
import com.qasmartpredict.backend.repository.SonarMetricRepository;
import com.qasmartpredict.backend.service.QisSonarIntegrationService;
import com.qasmartpredict.backend.service.SonarImportService;
import com.qasmartpredict.backend.service.SonarScoreService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


    /*
     * ==========================================================
     * ENREGISTRER MANUELLEMENT UNE ANALYSE SONAR
     * ==========================================================
     *
     * POST /api/sonar
     *
     * Le score Sonar est calculé automatiquement avant
     * l'enregistrement en base.
     */

    @PostMapping
    public ResponseEntity<SonarMetric> save(
            @RequestBody SonarMetric sonarMetric) {

        double score =
                sonarScoreService.calculateScore(
                        sonarMetric
                );

        sonarMetric.setSonarScore(score);

        SonarMetric saved =
                repository.save(sonarMetric);

        return ResponseEntity.ok(saved);
    }


    /*
     * ==========================================================
     * RECUPERER TOUTES LES ANALYSES SONAR
     * ==========================================================
     *
     * GET /api/sonar
     */

    @GetMapping
    public List<SonarMetric> findAll() {

        return repository.findAll();
    }


    /*
     * ==========================================================
     * RECUPERER UNE ANALYSE SONAR PAR ID
     * ==========================================================
     *
     * GET /api/sonar/{id}
     */

    @GetMapping("/{id}")
    public ResponseEntity<SonarMetric> findById(
            @PathVariable Long id) {

        return repository
                .findById(id)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }


    /*
     * ==========================================================
     * DERNIERE ANALYSE SONAR D'UN PROJET
     * ==========================================================
     *
     * GET /api/sonar/project/{projectId}/latest
     */

    @GetMapping("/project/{projectId}/latest")
    public ResponseEntity<SonarMetric> latestForProject(
            @PathVariable Long projectId) {

        return repository
                .findTopByProjectIdOrderByAnalysisDateDesc(
                        projectId
                )
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }


    /*
     * ==========================================================
     * IMPORT DU FICHIER SONAR STATIQUE
     * ==========================================================
     *
     * POST /api/sonar/import-static
     *
     * Paramètres :
     * projectId
     * releaseId
     */

    @PostMapping("/import-static")
    public ResponseEntity<SonarMetric> importStatic(
            @RequestParam Long projectId,
            @RequestParam Long releaseId) {

        try {

            SonarMetric sonarMetric =
                    sonarImportService
                            .importStaticSonarResult(
                                    projectId,
                                    releaseId
                            );

            return ResponseEntity.ok(
                    sonarMetric
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }


    /*
     * ==========================================================
     * QIS FINAL AVEC SONAR
     * ==========================================================
     *
     * GET /api/sonar/qis/{projectId}
     *
     * IMPORTANT :
     *
     * Le frontend ne fournit plus currentQis.
     *
     * Le backend :
     *
     * 1. calcule Coverage
     * 2. calcule Defect
     * 3. calcule Feedback
     * 4. calcule Incident
     * 5. calcule RCA
     * 6. calcule le QIS métier
     * 7. récupère le dernier Sonar Score
     * 8. calcule le QIS final
     *
     * QIS FINAL =
     *
     * QIS métier × 80 %
     * +
     * Sonar Score × 20 %
     */

    @GetMapping("/qis/{projectId}")
    public ResponseEntity<QisSonarResponse> calculateQisWithSonar(
            @PathVariable Long projectId) {

        QisSonarResponse result =
                qisSonarIntegrationService
                        .calculateFinalQis(
                                projectId
                        );

        return ResponseEntity.ok(
                result
        );
    }
}