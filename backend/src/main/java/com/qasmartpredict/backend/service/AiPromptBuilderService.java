package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.QualityIntelligenceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiPromptBuilderService {

    private final QualityIntelligenceService qisService;

    // Keep existing diagnostic endpoints /prompt and /raw-generate functional.
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

    public String buildPrompt(Map<String, Double> metrics) {
        double qis = requireMetric(metrics, "qis");
        double coverage = requireMetric(metrics, "coverageScore");
        double defect = requireMetric(metrics, "defectScore");
        double feedback = requireMetric(metrics, "feedbackScore");
        double incident = requireMetric(metrics, "incidentScore");

        String decision = decisionFor(qis);
        String risk = riskFor(coverage, defect);

        return """
                You are a Senior QA Manager. Analyze ONLY these quality scores (0 to 100):
                QIS: %.2f
                Coverage Quality Score: %.2f
                Defect Quality Score: %.2f
                Feedback Quality Score: %.2f
                Incident Quality Score: %.2f

                IMPORTANT: These are QUALITY scores, NOT counts or rates of incidents/defects.
                Higher scores are BETTER for every metric. For example, Incident Quality Score 97
                indicates GOOD production stability; Defect Quality Score 22.22 indicates POOR defect quality.
                Do not call a high incident quality score a high incident count.

                Deterministic release decision (DO NOT CHANGE): %s
                Deterministic risk level from coverage/defect thresholds (DO NOT CHANGE): %s
                Decision rules: QIS >= 80 GO; 60 <= QIS < 80 GO_WITH_RISK; QIS < 60 NO_GO.
                Risk rules: HIGH if coverage < 50 or defect < 50; MEDIUM if coverage < 70
                or defect < 70; otherwise LOW. (Other indicators may be evaluated by the
                richer AI Quality Advisor; do not claim this is a full risk assessment.)

                Return ONLY valid JSON with exactly these fields:
                {
                  "summary": "",
                  "executiveSummary": "",
                  "riskLevel": "%s",
                  "sprintDecision": "%s",
                  "strengths": [],
                  "weaknesses": [],
                  "recommendations": []
                }
                Use the exact riskLevel and sprintDecision supplied above.
                summary: one sentence; executiveSummary: 3 to 5 sentences.
                strengths, weaknesses, recommendations: arrays of strings.
                No markdown, no invented metrics, no other text.
                """.formatted(
                qis, coverage, defect, feedback, incident,
                decision, risk, risk, decision
        );
    }

    static double requireMetric(Map<String, Double> metrics, String key) {
        if (metrics == null || metrics.get(key) == null
                || !Double.isFinite(metrics.get(key))
                || metrics.get(key) < 0 || metrics.get(key) > 100) {
            throw new IllegalArgumentException("Invalid or missing metric: " + key);
        }
        return metrics.get(key);
    }

    public static String decisionFor(double qis) {
        return qis >= 80 ? "GO" : qis >= 60 ? "GO_WITH_RISK" : "NO_GO";
    }

    public static String riskFor(double coverage, double defect) {
        if (coverage < 50 || defect < 50) return "HIGH";
        if (coverage < 70 || defect < 70) return "MEDIUM";
        return "LOW";
    }
}
