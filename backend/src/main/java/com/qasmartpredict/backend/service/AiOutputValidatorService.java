package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.AiGenerativeAnalysisDto;
import com.qasmartpredict.backend.dto.AiIntelligenceRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
public class AiOutputValidatorService {

    public record ValidationResult(String status, List<String> issues) {}

    /** Checks structural and rule-based consistency, not the meaning of free text. */
    public ValidationResult validate(AiIntelligenceRequest request,
                                     AiGenerativeAnalysisDto analysis,
                                     String officialRiskLevel,
                                     String officialReleaseDecision) {
        List<String> issues = new ArrayList<>();
        if (request == null) {
            issues.add("Missing source QA metrics");
            return new ValidationResult("FAIL", List.copyOf(issues));
        }
        if (analysis == null) {
            issues.add("Missing Llama analysis");
            return new ValidationResult("FAIL", List.copyOf(issues));
        }

        requireText(analysis.getExecutiveSummary(), "executiveSummary", issues);
        requireText(analysis.getReleaseRationale(), "releaseRationale", issues);
        requireText(analysis.getManagerMessage(), "managerMessage", issues);
        requireText(analysis.getConfidenceExplanation(), "confidenceExplanation", issues);

        if (analysis.getTopRisks() == null) {
            issues.add("Missing topRisks array");
        } else {
            for (int i = 0; i < analysis.getTopRisks().size(); i++) {
                var risk = analysis.getTopRisks().get(i);
                if (risk == null) {
                    issues.add("Null topRisks[" + i + "]");
                    continue;
                }
                requireText(risk.getDimension(), "topRisks[" + i + "].dimension", issues);
                requireText(risk.getReason(), "topRisks[" + i + "].reason", issues);
                if (risk.getSeverity() == null || !Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL").contains(risk.getSeverity())) {
                    issues.add("Invalid topRisks[" + i + "].severity");
                }
            }
        }

        if (analysis.getPriorityActions() == null) {
            issues.add("Missing priorityActions array");
        } else {
            Set<Integer> usedPriorities = new HashSet<>();
            for (int i = 0; i < analysis.getPriorityActions().size(); i++) {
                var action = analysis.getPriorityActions().get(i);
                if (action == null) {
                    issues.add("Null priorityActions[" + i + "]");
                    continue;
                }
                requireText(action.getAction(), "priorityActions[" + i + "].action", issues);
                requireText(action.getExpectedImpact(), "priorityActions[" + i + "].expectedImpact", issues);
                if (action.getPriority() <= 0) {
                    issues.add("Invalid priorityActions[" + i + "].priority: must be positive");
                } else if (!usedPriorities.add(action.getPriority())) {
                    issues.add("Duplicate action priority: " + action.getPriority());
                }
            }
        }

        if ("HIGH".equalsIgnoreCase(officialRiskLevel)
                || "CRITICAL".equalsIgnoreCase(officialRiskLevel)) {
            if (analysis.getTopRisks() == null || analysis.getTopRisks().isEmpty()) {
                issues.add("High-risk release must identify at least one top risk");
            }
            if (analysis.getPriorityActions() == null || analysis.getPriorityActions().isEmpty()) {
                issues.add("High-risk release must propose at least one priority action");
            }
        }

        if (!Double.isFinite(request.getQis()) || request.getQis() < 0 || request.getQis() > 100) {
            issues.add("Source QIS outside expected 0..100 range");
        }
        if (officialRiskLevel == null || officialRiskLevel.isBlank()) {
            issues.add("Missing official risk level");
        }
        if (officialReleaseDecision == null || officialReleaseDecision.isBlank()) {
            issues.add("Missing official release decision");
        }
        return new ValidationResult(issues.isEmpty() ? "PASS" : "FAIL", List.copyOf(issues));
    }

    private static void requireText(String value, String field, List<String> issues) {
        if (value == null || value.isBlank()) issues.add("Missing or blank " + field);
    }
}
