package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

/** Deterministic guardrails. Does not certify the semantic quality of an LLM answer. */
@Service
public class AiRevisionValidationService {
    private static final Pattern MODERATE_RISK = Pattern.compile(
            "(?i)\\b(?:main\\s+)?risk\\b[^.!?]{0,100}\\bmoderate\\b|\\bmoderate\\b[^.!?]{0,60}\\brisk\\b");
    private static final List<String> REQUIRED = List.of(
            "executiveSummary", "releaseRationale", "topRisks", "priorityActions",
            "managerMessage", "confidenceExplanation", "revision");

    public Map<String,Object> validate(JsonNode official, JsonNode original, JsonNode revised) {
        List<Map<String,String>> issues = new ArrayList<>();
        if (!revised.isObject()) {
            issues.add(issue("INVALID_RESPONSE", "root", "Expected a JSON object"));
            return report(issues, original, revised);
        }
        for (String field : REQUIRED) if (!revised.has(field) || revised.path(field).isNull())
            issues.add(issue("MISSING_FIELD", field, "Required field missing"));
        for (String field : List.of("executiveSummary", "releaseRationale", "managerMessage", "confidenceExplanation")) {
            if (!revised.path(field).isTextual() || revised.path(field).asText().isBlank())
                issues.add(issue("INVALID_FIELD", field, "Expected non-empty string"));
        }
        for (String field : List.of("topRisks", "priorityActions"))
            if (!revised.path(field).isArray()) issues.add(issue("INVALID_FIELD", field, "Expected array"));
        JsonNode revision = revised.path("revision");
        if (!revision.isObject()) issues.add(issue("INVALID_FIELD", "revision", "Expected object"));
        else for (String field : List.of("correctedIssues", "acceptedCritiques", "rejectedCritiques",
                "unverifiedClaims", "residualRisks", "requiredEvidence"))
            if (!revision.path(field).isArray())
                issues.add(issue("INVALID_FIELD", "revision."+field, "Expected array"));
        String officialRisk = official.path("riskLevel").asText("");
        if (Set.of("HIGH", "CRITICAL").contains(officialRisk.toUpperCase(Locale.ROOT))) {
            for (String field : List.of("executiveSummary", "releaseRationale", "managerMessage")) {
                String text = revised.path(field).asText("");
                if (MODERATE_RISK.matcher(text).find())
                    issues.add(issue("OFFICIAL_RISK_CONTRADICTION", field,
                            "Official risk is "+officialRisk+" but narrative describes risk as moderate"));
            }
        }
        if (official.has("defectScore") && revised.toString().toLowerCase(Locale.ROOT).contains("defect quality ratio"))
            issues.add(issue("UNVERIFIED_METRIC_SEMANTICS", "defectScore",
                    "Defect score must not be called a defect ratio without an authoritative formula"));
        if (revised.has("releaseDecision") && !official.path("releaseDecision").asText("")
                .equals(revised.path("releaseDecision").asText("")))
            issues.add(issue("OFFICIAL_DECISION_OVERRIDE", "releaseDecision", "Revision changes official decision"));
        if (revised.has("qis") && revised.path("qis").asDouble(Double.NaN) != official.path("qis").asDouble(Double.NaN))
            issues.add(issue("OFFICIAL_QIS_OVERRIDE", "qis", "Revision changes official QIS"));
        return report(issues, original, revised);
    }

    private Map<String,Object> report(List<Map<String,String>> issues, JsonNode original, JsonNode revised) {
        boolean originalContradiction = containsModerateRisk(original);
        boolean revisedContradiction = containsModerateRisk(revised);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("status", issues.isEmpty() ? "PASS_GUARDRAILS" : "REQUIRES_REVIEW");
        result.put("issues", issues);
        result.put("v1ModerateRiskWording", originalContradiction);
        result.put("v2ModerateRiskWording", revisedContradiction);
        result.put("riskWordingImproved", originalContradiction && !revisedContradiction);
        result.put("semanticQualityCertified", false);
        result.put("note", "Passing deterministic checks does not establish factual completeness or semantic correctness");
        return result;
    }
    private boolean containsModerateRisk(JsonNode node) {
        for (String field : List.of("executiveSummary", "releaseRationale", "managerMessage"))
            if (MODERATE_RISK.matcher(node.path(field).asText("")).find()) return true;
        return false;
    }
    private Map<String,String> issue(String type, String field, String message) {
        return Map.of("type",type,"field",field,"message",message);
    }
}
