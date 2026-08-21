package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.AiForecastDto;
import com.qasmartpredict.backend.dto.AiIntelligenceRequest;
import com.qasmartpredict.backend.dto.AiIntelligenceResponse;

import lombok.RequiredArgsConstructor;
import com.qasmartpredict.backend.dto.AiGenerativeAnalysisDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiIntelligenceService {
	
	private final OpenAiQaService openAiQaService;

    public AiIntelligenceResponse analyze(
            AiIntelligenceRequest request) {

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        /*
         * COVERAGE
         */

        if (request.getCoverageScore() >= 80) {

            strengths.add(
                    "Functional coverage is strong."
            );

        } else {

            weaknesses.add(
                    "Functional coverage is below the recommended level."
            );

            recommendations.add(
                    "Increase functional test coverage."
            );
        }


        /*
         * DEFECT QUALITY
         */

        if (request.getDefectScore() < 60) {

            weaknesses.add(
                    "Defect quality represents a significant release risk."
            );

            recommendations.add(
                    "Prioritize unresolved high-risk defects."
            );

        } else {

            strengths.add(
                    "Defect quality remains under control."
            );
        }


        /*
         * FEEDBACK
         */

        if (request.getFeedbackScore() >= 80) {

            strengths.add(
                    "Feedback quality is strong."
            );

        } else {

            weaknesses.add(
                    "Feedback quality requires attention."
            );
        }


        /*
         * INCIDENT
         */

        if (request.getIncidentScore() >= 80) {

            strengths.add(
                    "Production stability is good."
            );

        } else {

            weaknesses.add(
                    "Production incidents indicate stability risks."
            );

            recommendations.add(
                    "Investigate recurring production incidents."
            );
        }


        /*
         * SONAR
         */

        if (request.getSonarScore() >= 70) {

            strengths.add(
                    "Sonar technical quality is acceptable."
            );

        } else {

            weaknesses.add(
                    "Technical quality measured by Sonar is weak."
            );
        }


        if (
                request.getVulnerabilities() != null
                &&
                request.getVulnerabilities() > 0
        ) {

            weaknesses.add(
                    request.getVulnerabilities()
                            + " Sonar vulnerability detected."
            );

            recommendations.add(
                    "Resolve Sonar vulnerabilities before release."
            );
        }


        if (
                request.getCodeSmells() != null
                &&
                request.getCodeSmells() > 30
        ) {

            weaknesses.add(
                    "Technical debt is elevated because of code smells."
            );

            recommendations.add(
                    "Reduce code smells and technical debt."
            );
        }


        /*
         * GLOBAL DECISION
         */

        String decision =
                calculateDecision(request.getQis());

        String risk =
                calculateRisk(request);


        String mainRisk =
                identifyMainRisk(request);


        int confidence =
                calculateConfidence(request);


        /*
         * SIMPLE FORECAST V1
         */

        double predictedQis =
                calculateForecast(request);

        AiForecastDto forecast =
                AiForecastDto.builder()
                        .predictedNextQis(predictedQis)
                        .trend(
                                predictedQis > request.getQis()
                                        ? "IMPROVING"
                                        : "STABLE"
                        )
                        .expectedReleaseStatus(
                                calculateDecision(predictedQis)
                        )
                        .build();


        String summary =
                buildSummary(
                        request,
                        mainRisk,
                        decision
                );


        String executiveSummary =
                buildExecutiveSummary(
                        request,
                        mainRisk,
                        decision,
                        forecast
                );

        
		
AiGenerativeAnalysisDto aiAnalysis;

try {

    aiAnalysis =
            openAiQaService.generateAnalysis(
                    request,
                    decision,
                    risk,
                    mainRisk,
                    confidence
            );

} catch (Exception e) {

    System.err.println(
            "OpenAI error: " + e.getMessage()
    );

    aiAnalysis =
            AiGenerativeAnalysisDto.builder()

                    .executiveSummary(
                            executiveSummary
                    )

                    .releaseRationale(
                            "Generative AI analysis unavailable. "
                            + "Deterministic SmartPredict decision remains valid."
                    )

                    .topRisks(
                            List.of()
                    )

                    .priorityActions(
                            List.of()
                    )

                    .managerMessage(
                            "AI analysis temporarily unavailable."
                    )

                    .confidenceExplanation(
                            "Confidence is based on available QA metrics."
                    )

                    .build();
}



        return AiIntelligenceResponse.builder()

        .qis(request.getQis())

        .riskLevel(risk)

        .releaseDecision(decision)

        .confidence(confidence)

        .mainRisk(mainRisk)

        .summary(summary)

        .executiveSummary(
                aiAnalysis.getExecutiveSummary()
        )

        .strengths(strengths)

        .weaknesses(weaknesses)

        .recommendations(recommendations)

        .forecast(forecast)

        .aiAnalysis(aiAnalysis)

        .build();
    }


    private String calculateDecision(double qis) {

        if (qis >= 80) {
            return "GO";
        }

        if (qis >= 60) {
            return "GO_WITH_RISK";
        }

        return "NO_GO";
    }


    private String calculateRisk(
            AiIntelligenceRequest request) {

        if (
                request.getDefectScore() < 50
                ||
                request.getCoverageScore() < 50
        ) {

            return "HIGH";
        }

        if (
                request.getDefectScore() < 70
                ||
                request.getCoverageScore() < 70
                ||
                (
                        request.getVulnerabilities() != null
                        &&
                        request.getVulnerabilities() > 0
                )
        ) {

            return "MEDIUM";
        }

        return "LOW";
    }


    private String identifyMainRisk(
            AiIntelligenceRequest request) {

        double minimum =
                Math.min(
                        request.getCoverageScore(),
                        request.getDefectScore()
                );

        minimum =
                Math.min(
                        minimum,
                        request.getSonarScore()
                );


        if (minimum == request.getDefectScore()) {

            return "DEFECT_QUALITY";
        }

        if (minimum == request.getCoverageScore()) {

            return "COVERAGE";
        }

        return "SONAR_QUALITY";
    }


    private int calculateConfidence(
            AiIntelligenceRequest request) {

        int confidence = 80;

        if (request.getTotalRequirements() != null) {
            confidence += 5;
        }

        if (request.getTotalStories() != null) {
            confidence += 5;
        }

        if (request.getSonarScore() > 0) {
            confidence += 5;
        }

        return Math.min(confidence, 95);
    }


    private double calculateForecast(
            AiIntelligenceRequest request) {

        /*
         * Forecast temporaire V1.
         *
         * Plus tard :
         * historique QualitySnapshot
         * + modèle prédictif.
         */

        double potentialImprovement = 0;

        if (request.getCoverageScore() < 80) {
            potentialImprovement += 1.5;
        }

        if (request.getDefectScore() < 70) {
            potentialImprovement += 1.5;
        }

        return Math.round(
                (request.getQis()
                        + potentialImprovement)
                        * 100.0
        ) / 100.0;
    }


    private String buildSummary(
            AiIntelligenceRequest request,
            String mainRisk,
            String decision) {

        return "Current QIS is "
                + request.getQis()
                + "/100. Main quality risk: "
                + mainRisk
                + ". Release recommendation: "
                + decision
                + ".";
    }


    private String buildExecutiveSummary(
            AiIntelligenceRequest request,
            String mainRisk,
            String decision,
            AiForecastDto forecast) {

        return "The project currently has a Quality Intelligence Score of "
                + request.getQis()
                + ". The primary risk is "
                + mainRisk
                + ". The recommended release decision is "
                + decision
                + ". Based on the current quality indicators, "
                + "the next predicted QIS is approximately "
                + forecast.getPredictedNextQis()
                + ".";
    }
}