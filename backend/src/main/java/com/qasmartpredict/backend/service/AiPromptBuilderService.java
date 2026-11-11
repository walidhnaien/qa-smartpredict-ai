package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.QualityIntelligenceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiPromptBuilderService {

    private final QualityIntelligenceService qisService;

    // Maintains compatibility with GET /api/ai/prompt and /raw-generate.
    public String buildPrompt() {
        QualityIntelligenceDto qis = qisService.calculate();
        return buildPrompt(Map.of(
                "qis", qis.getQualityIntelligenceScore(),
                "coverageScore", qis.getCoverageScore(),
                "defectScore", qis.getDefectScore(),
                "feedbackScore", qis.getFeedbackScore(),
                "incidentScore", qis.getIncidentScore()
        ));
    }

    // Uses metrics supplied by the selected release; does not recalculate QIS.
    public String buildPrompt(Map<String, Double> metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("AI metrics are required");
        }
        for (String key : new String[]{"qis", "coverageScore", "defectScore", "feedbackScore", "incidentScore"}) {
            Double value = metrics.get(key);
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Missing or invalid AI metric: " + key);
            }
        }
        return """
                You are a Senior QA Manager.
                Analyze ONLY the following metrics for the currently selected release:

                Quality Intelligence Score: %s
                Coverage Score: %s
                Defect Score: %s
                Feedback Score: %s
                Incident Score: %s

                Return ONLY a valid JSON object with exactly these fields:
                {
                  "summary": "",
                  "executiveSummary": "",
                  "riskLevel": "",
                  "sprintDecision": "",
                  "strengths": [],
                  "weaknesses": [],
                  "recommendations": []
                }

                Rules:
                - Never invent or change the supplied metric values.
                - riskLevel must be one of: LOW, MEDIUM, HIGH
                - sprintDecision must be one of: GO, GO WITH RISKS, NO GO
                - Do not claim the decision is deterministic: these inputs do not contain the authoritative release decision.
                - summary must be one short sentence.
                - executiveSummary must contain 3 to 5 professional sentences.
                - strengths, weaknesses and recommendations must be arrays of strings.
                - Return JSON only, without markdown or explanations.
                """.formatted(
                metrics.get("qis"),
                metrics.get("coverageScore"),
                metrics.get("defectScore"),
                metrics.get("feedbackScore"),
                metrics.get("incidentScore")
        );
    }
}
