package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.QisSonarResponse;
import com.qasmartpredict.backend.dto.QualityIntelligenceDto;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReleaseAnalysisService {

    private final QualityIntelligenceService
            qualityIntelligenceService;

    private final QisSonarIntegrationService
            qisSonarIntegrationService;

    private final QualitySnapshotService
            qualitySnapshotService;


    public QisSonarResponse analyzeRelease(
            UUID releaseId) {

        /*
         * 1. Calcul des KPI métier de la Release
         */
        QualityIntelligenceDto quality =
                qualityIntelligenceService
                        .calculate(releaseId);


        /*
         * 2. Calcul du QIS final avec Sonar
         */
        QisSonarResponse finalQuality =
                qisSonarIntegrationService
                        .calculateFinalQisByRelease(
                                1L,
                                releaseId
                        );


        /*
         * 3. Sonar peut être absent.
         */
        double sonarScore =
                finalQuality.getSonarScore() != null
                        ? finalQuality.getSonarScore()
                        : 0.0;


        /*
         * 4. Sauvegarde du snapshot
         */
        qualitySnapshotService.saveSnapshot(
                releaseId,

                finalQuality.getFinalQis(),

                quality.getCoverageScore(),

                quality.getDefectScore(),

                quality.getFeedbackScore(),

                quality.getIncidentScore(),

                sonarScore
        );


        /*
         * 5. Retour du résultat
         */
        return finalQuality;
    }
}