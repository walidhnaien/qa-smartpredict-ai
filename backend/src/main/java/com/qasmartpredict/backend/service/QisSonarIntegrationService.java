package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.QisSonarResponse;
import com.qasmartpredict.backend.dto.QualityIntelligenceDto;
import com.qasmartpredict.backend.dto.SonarMetric;
import com.qasmartpredict.backend.repository.SonarMetricRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QisSonarIntegrationService {

    private final SonarMetricRepository sonarMetricRepository;

    /*
     * Service qui calcule automatiquement :
     *
     * Coverage
     * Defect
     * Feedback
     * Incident
     * RCA
     *
     * puis le QIS métier.
     */
    private final QualityIntelligenceService qualityIntelligenceService;


    /*
     * Poids Sonar dans le QIS final.
     *
     * Valeur par défaut = 20 %
     */
    @Value("${qa.qis.sonar-weight:0.20}")
    private double sonarWeight;


    /*
     * ==========================================================
     * CALCUL AUTOMATIQUE DU QIS FINAL
     * ==========================================================
     *
     * Cette méthode est celle utilisée par le Controller.
     *
     * Le frontend ne fournit plus le currentQis.
     *
     * Le backend calcule lui-même le QIS métier.
     */

    public QisSonarResponse calculateFinalQis(
            Long projectId) {

        QualityIntelligenceDto quality =
                qualityIntelligenceService.calculate();

        double businessQis =
                quality.getQualityIntelligenceScore();

        return calculateFinalQis(
                projectId,
                businessQis
        );
    }


    /*
     * ==========================================================
     * CALCUL QIS + SONAR
     * ==========================================================
     *
     * Cette méthode réalise le calcul :
     *
     * QIS FINAL =
     *
     * QIS métier × 80 %
     * +
     * Sonar Score × 20 %
     *
     * On conserve cette méthode séparée pour garder
     * la logique de calcul réutilisable.
     */

    public QisSonarResponse calculateFinalQis(
            Long projectId,
            double currentQis) {

        /*
         * Récupération de la dernière analyse Sonar
         * du projet.
         */

        SonarMetric sonar =
                sonarMetricRepository
                        .findTopByProjectIdOrderByAnalysisDateDesc(
                                projectId
                        )
                        .orElse(null);


        /*
         * ======================================================
         * AUCUNE ANALYSE SONAR
         * ======================================================
         *
         * Si aucune analyse Sonar n'existe,
         * le QIS métier devient le QIS final.
         */

        if (sonar == null) {

            return QisSonarResponse.builder()

                    .projectId(projectId)

                    .baseQis(
                            round(currentQis)
                    )

                    .sonarScore(null)

                    .sonarWeight(0.0)

                    .sonarContribution(0.0)

                    .baseQisContribution(
                            round(currentQis)
                    )

                    .finalQis(
                            round(currentQis)
                    )

                    .sonarQualityGate(
                            "NOT_AVAILABLE"
                    )

                    .build();
        }


        /*
         * ======================================================
         * SCORE SONAR
         * ======================================================
         */

        double sonarScore =
                sonar.getSonarScore() != null
                        ? sonar.getSonarScore()
                        : 0.0;


        /*
         * ======================================================
         * PONDERATIONS
         * ======================================================
         *
         * Sonar = 20 %
         * Métier = 80 %
         */

        double baseWeight =
                1.0 - sonarWeight;


        /*
         * ======================================================
         * CONTRIBUTION DU QIS METIER
         * ======================================================
         */

        double baseContribution =
                currentQis * baseWeight;


        /*
         * ======================================================
         * CONTRIBUTION SONAR
         * ======================================================
         */

        double sonarContribution =
                sonarScore * sonarWeight;


        /*
         * ======================================================
         * QIS FINAL
         * ======================================================
         */

        double finalQis =
                baseContribution
                        + sonarContribution;


        /*
         * ======================================================
         * REPONSE
         * ======================================================
         */

        return QisSonarResponse.builder()

                .projectId(projectId)

                .baseQis(
                        round(currentQis)
                )

                .sonarScore(
                        round(sonarScore)
                )

                .sonarWeight(
                        sonarWeight
                )

                .baseQisContribution(
                        round(baseContribution)
                )

                .sonarContribution(
                        round(sonarContribution)
                )

                .finalQis(
                        round(finalQis)
                )

                .sonarQualityGate(
                        sonar.getQualityGate()
                )

                .bugs(
                        sonar.getBugs()
                )

                .vulnerabilities(
                        sonar.getVulnerabilities()
                )

                .codeSmells(
                        sonar.getCodeSmells()
                )

                .coverage(
                        sonar.getCoverage()
                )

                .duplicatedLinesDensity(
                        sonar.getDuplicatedLinesDensity()
                )

                .build();
    }


    /*
     * ==========================================================
     * ARRONDI
     * ==========================================================
     */

    private double round(double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}