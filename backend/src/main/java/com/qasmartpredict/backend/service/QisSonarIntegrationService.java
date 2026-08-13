package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.QisSonarResponse;
import com.qasmartpredict.backend.dto.SonarMetric;
import com.qasmartpredict.backend.repository.SonarMetricRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QisSonarIntegrationService {

    private final SonarMetricRepository sonarMetricRepository;

    @Value("${qa.qis.sonar-weight:0.20}")
    private double sonarWeight;

    public QisSonarResponse calculateFinalQis(
            Long projectId,
            double currentQis) {

        SonarMetric sonar =
                sonarMetricRepository
                        .findTopByProjectIdOrderByAnalysisDateDesc(projectId)
                        .orElse(null);

        /*
         * Si aucune analyse Sonar n'existe,
         * on conserve le QIS fonctionnel actuel.
         */
        if (sonar == null) {

            return QisSonarResponse.builder()
                    .projectId(projectId)
                    .baseQis(currentQis)
                    .sonarScore(null)
                    .sonarWeight(0.0)
                    .sonarContribution(0.0)
                    .baseQisContribution(currentQis)
                    .finalQis(currentQis)
                    .sonarQualityGate("NOT_AVAILABLE")
                    .build();
        }

        double sonarScore =
                sonar.getSonarScore() != null
                        ? sonar.getSonarScore()
                        : 0.0;

        double baseWeight =
                1.0 - sonarWeight;

        double baseContribution =
                currentQis * baseWeight;

        double sonarContribution =
                sonarScore * sonarWeight;

        double finalQis =
                baseContribution
                        + sonarContribution;

        return QisSonarResponse.builder()
                .projectId(projectId)

                .baseQis(round(currentQis))

                .sonarScore(round(sonarScore))

                .sonarWeight(sonarWeight)

                .baseQisContribution(
                        round(baseContribution))

                .sonarContribution(
                        round(sonarContribution))

                .finalQis(
                        round(finalQis))

                .sonarQualityGate(
                        sonar.getQualityGate())

                .bugs(
                        sonar.getBugs())

                .vulnerabilities(
                        sonar.getVulnerabilities())

                .codeSmells(
                        sonar.getCodeSmells())

                .coverage(
                        sonar.getCoverage())

                .duplicatedLinesDensity(
                        sonar.getDuplicatedLinesDensity())

                .build();
    }

    private double round(double value) {

        return Math.round(value * 100.0) / 100.0;
    }
}