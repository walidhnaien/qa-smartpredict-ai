package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.SonarMetric;
import org.springframework.stereotype.Service;

@Service
public class SonarScoreService {

    public double calculateScore(SonarMetric sonar) {

        if (sonar == null) {
            return 0.0;
        }

        double bugScore =
                calculateBugScore(sonar.getBugs());

        double vulnerabilityScore =
                calculateVulnerabilityScore(
                        sonar.getVulnerabilities());

        double codeSmellScore =
                calculateCodeSmellScore(
                        sonar.getCodeSmells());

        double duplicationScore =
                calculateDuplicationScore(
                        sonar.getDuplicatedLinesDensity());

        /*
         * Pondération interne Sonar :
         *
         * Bugs             35 %
         * Vulnerabilities  30 %
         * Code Smells      20 %
         * Duplication      15 %
         */

        double score =
                bugScore * 0.35
                + vulnerabilityScore * 0.30
                + codeSmellScore * 0.20
                + duplicationScore * 0.15;

        return round(score);
    }

    private double calculateBugScore(Integer bugs) {

        if (bugs == null) {
            return 100;
        }

        if (bugs == 0) return 100;
        if (bugs <= 2) return 90;
        if (bugs <= 5) return 75;
        if (bugs <= 10) return 50;
        if (bugs <= 20) return 25;

        return 0;
    }

    private double calculateVulnerabilityScore(
            Integer vulnerabilities) {

        if (vulnerabilities == null) {
            return 100;
        }

        if (vulnerabilities == 0) return 100;
        if (vulnerabilities == 1) return 75;
        if (vulnerabilities <= 3) return 50;
        if (vulnerabilities <= 5) return 25;

        return 0;
    }

    private double calculateCodeSmellScore(
            Integer codeSmells) {

        if (codeSmells == null) {
            return 100;
        }

        if (codeSmells <= 5) return 100;
        if (codeSmells <= 20) return 85;
        if (codeSmells <= 50) return 65;
        if (codeSmells <= 100) return 40;

        return 20;
    }

    private double calculateDuplicationScore(
            Double duplication) {

        if (duplication == null) {
            return 100;
        }

        if (duplication <= 3) return 100;
        if (duplication <= 5) return 90;
        if (duplication <= 10) return 70;
        if (duplication <= 20) return 40;

        return 10;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}