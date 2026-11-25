package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

/** Deterministic evidence checks and QA investigation plan; does not certify LLM opinions. */
@Service
public class AiEvidenceIntelligenceService {
    private final AiAgentRecommendationService recommendationService;

    public AiEvidenceIntelligenceService(AiAgentRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    private static final Pattern MODERATE = Pattern.compile("(?i)\\b(?:main\\s+)?risk\\b[^.!?]{0,100}\\bmoderate\\b|\\bmoderate\\b[^.!?]{0,60}\\brisk\\b");

    public Map<String,Object> assess(JsonNode source, JsonNode candidate, List<JsonNode> reviews) {
        List<Map<String,Object>> checks = new ArrayList<>();
        List<Map<String,Object>> actions = new ArrayList<>();
        List<Map<String,Object>> hypotheses = new ArrayList<>();
        List<Map<String,Object>> reviewerChecks = new ArrayList<>();
        String officialRisk = source.path("riskLevel").asText("");
        for (String field : List.of("executiveSummary", "releaseRationale", "managerMessage")) {
            String text = candidate.path(field).asText("");
            if (Set.of("HIGH", "CRITICAL").contains(officialRisk.toUpperCase(Locale.ROOT)) && MODERATE.matcher(text).find()) {
                checks.add(item("type", "RISK_LEVEL_CONTRADICTION", "field", field,
                        "officialValue", officialRisk, "evidence", text, "status", "REQUIRES_REVIEW"));
            }
        }
        int totalReq = source.path("totalRequirements").asInt(-1);
        int uncovered = source.path("uncoveredRequirements").asInt(-1);
        int totalBugs = source.path("totalBugs").asInt(-1);
        int smells = source.path("codeSmells").asInt(-1);
        if (totalReq > 0 && uncovered > 0) {
            actions.add(action(1, "TRACEABILITY", "Identify the uncovered requirement and assess business criticality; add missing acceptance and regression tests", "totalRequirements="+totalReq+", uncoveredRequirements="+uncovered, "Requirement identified, criticality documented, missing tests linked"));
            hypotheses.add(item("hypothesis", "The uncovered requirement may affect a critical business journey", "evidence", "uncoveredRequirements="+uncovered, "status", "UNVERIFIED", "neededData", "Requirement identity, criticality and affected user journeys"));
        }
        if (totalBugs > 0) {
            actions.add(action(1, "DEFECT_RISK", "Triage the recorded defects by severity, status, affected component and customer impact; confirm release blockers", "totalBugs="+totalBugs, "Each defect has severity, owner, release disposition and rationale"));
            hypotheses.add(item("hypothesis", "Defects may be concentrated in a high-impact component", "evidence", "totalBugs="+totalBugs, "status", "UNVERIFIED", "neededData", "Defect-to-component mapping and severity"));
        }
        if (smells > 0) {
            actions.add(action(2, "MAINTAINABILITY", "Inspect Sonar code-smell hotspots and correlate with defect locations only if component mappings exist", "codeSmells="+smells, "Hotspots reviewed; any correlation supported by linked component evidence"));
        }
        if ("GO_WITH_RISK".equalsIgnoreCase(source.path("releaseDecision").asText(""))) {
            actions.add(action(1, "RELEASE_GOVERNANCE", "Document residual risks, mitigations, acceptance owner and rollback criteria before release", "releaseDecision=GO_WITH_RISK", "Risk acceptance and rollback criteria recorded"));
        }
        // A score is not automatically a defect percentage. Flag this ambiguous wording for definition review.
        if (source.has("defectScore") && candidate.toString().toLowerCase(Locale.ROOT).contains("defect quality ratio")) {
            checks.add(item("type", "METRIC_SEMANTICS_AMBIGUOUS", "field", "defectScore", "officialValue", source.get("defectScore"), "status", "REQUIRES_DEFINITION", "evidence", "Candidate calls defectScore a defect quality ratio; confirm formula and direction before interpreting it"));
        }
        int riskIndex = 0;
        for (JsonNode risk : candidate.path("topRisks")) {
            String reason = risk.path("reason").asText("");
            if (source.path("defectScore").isNumber() && Pattern.compile(
                    "(?i)higher than expected defect quality score|high defect quality score indicates (?:potential )?(?:issues|poor|weak)").matcher(reason).find()) {
                checks.add(item("type", "DEFECT_SCORE_DIRECTION", "field", "defectScore",
                        "sourcePath", "/topRisks/" + riskIndex + "/reason", "officialValue", source.get("defectScore"),
                        "evidence", reason, "status", "REQUIRES_REVIEW"));
            }
            riskIndex++;
        }
        String confidenceText = candidate.path("confidenceExplanation").asText("");
        if (source.hasNonNull("confidence") && Pattern.compile(
                "(?i)high confidence[^.!?]{0,100}supports the (?:deterministic )?release decision").matcher(confidenceText).find()) {
            checks.add(item("type", "CONFIDENCE_RELEASE_JUSTIFICATION", "field", "confidenceExplanation",
                    "sourcePath", "/confidenceExplanation", "officialValue", source.get("confidence"),
                    "evidence", confidenceText, "status", "REQUIRES_REVIEW"));
        }
        boolean candidateMentionsDefectRisk = candidate.path("topRisks").isArray() &&
                containsRiskDimension(candidate.path("topRisks"), "DEFECT_QUALITY");
        for (int i=0; i<reviews.size(); i++) {
            JsonNode review = reviews.get(i);
            String reviewer = review.path("reviewerModel").asText("review-"+i);
            JsonNode analysis = review.has("analysis") ? review.path("analysis") : review;
            for (JsonNode statement : analysis.path("missingRisks")) {
                if (!statement.isTextual()) continue;
                String s = statement.asText().toLowerCase(Locale.ROOT);
                if (candidateMentionsDefectRisk && (s.contains("does not mention any risks related to") || s.contains("no risk related to")) && (s.contains("defect") || s.contains("quality"))) {
                    reviewerChecks.add(item("reviewer", reviewer, "type", "POTENTIAL_REVIEWER_FALSE_POSITIVE", "statement", statement.asText(), "evidence", "candidate.topRisks contains DEFECT_QUALITY", "status", "REQUIRES_REVIEW"));
                }
            }
        }
        List<Map<String,Object>> nliAssessments = new ArrayList<>();
        Map<String,Map<String,Object>> discoveries = new LinkedHashMap<>();
        for(JsonNode wrapper:reviews) {
            String model=wrapper.path("reviewerModel").asText("unknown");
            for(JsonNode a:wrapper.path("analysis").path("assessments")) nliAssessments.add(item("model",model,"assessment",a));
            for(JsonNode f:wrapper.path("analysis").path("newFindings")) {
                // Only expose verified quotes; check against persisted candidate again.
                if(!"QUOTE_VERIFIED".equals(f.path("evidenceStatus").asText())) continue;
                if(!candidate.at(f.path("sourcePath").asText()).asText().contains(f.path("evidence").asText())) continue;
                String key=f.path("sourcePath").asText()+"|"+f.path("evidence").asText()+"|"+f.path("hypothesis").asText();
                Map<String,Object> entry=discoveries.computeIfAbsent(key,k->item("sourcePath",f.path("sourcePath").asText(),"evidence",f.path("evidence").asText(),"status","HUMAN_REVIEW_REQUIRED","contributions",new ArrayList<Map<String,Object>>()));
                @SuppressWarnings("unchecked") List<Map<String,Object>> contributions=(List<Map<String,Object>>)entry.get("contributions");
                contributions.add(item("model",model,"category",f.path("category").asText(),"reason",f.path("reason").asText(),"relation",f.path("relation").asText(),"referenceId",f.path("referenceId").asText(),"premise",f.path("premise").asText(),"hypothesis",f.path("hypothesis").asText(),"claimType",f.path("claimType").asText()));
            }
        }
        actions.sort(Comparator.comparingInt(a -> (int)a.get("priority")));
        List<Map<String,Object>> proposals = agentActionCandidates(source, candidate, reviews);
        Map<String,Object> agentAssessment = recommendationService.evaluate(source, proposals);
        return item("objectiveChecks", checks, "reviewerChecks", reviewerChecks, "discoveryFindings", discoveries.values(), "nliAssessments", nliAssessments, "prioritizedActions", actions,
                "hypotheses", hypotheses, "agentActionCandidates", proposals, "agentRecommendationAssessment", agentAssessment, "dataGaps", dataGaps(source), "officialDecision", source.path("releaseDecision").asText("UNKNOWN"),
                "certification", "ONLY_DETERMINISTIC_FIELDS_ARE_OFFICIAL", "semanticReviewStatus", "HUMAN_REVIEW_REQUIRED");
    }
    /** Preserve each LLM's contribution and its exact source. These are proposals, not certified actions. */
    private static List<Map<String,Object>> agentActionCandidates(JsonNode source, JsonNode candidate, List<JsonNode> reviews) {
        List<Map<String,Object>> proposals = new ArrayList<>();
        JsonNode llamaActions = candidate.path("priorityActions");
        if (llamaActions.isArray()) {
            for (int i = 0; i < llamaActions.size(); i++) {
                JsonNode a = llamaActions.get(i);
                String value = a.path("action").asText("").trim();
                if (!value.isEmpty()) {
                    proposals.add(item("origin", "LLAMA", "sourcePath", "/candidateAnalysis/priorityActions/" + i,
                            "proposal", value, "modelPriority", a.path("priority").isIntegralNumber() ? a.path("priority").asInt() : null,
                            "expectedImpactClaim", a.path("expectedImpact").asText(null),
                            "evidenceStatus", "NOT_VERIFIED", "reviewStatus", "REQUIRES_QA_MANAGER_REVIEW"));
                }
            }
        }
        for (JsonNode wrapper : reviews) {
            String model = wrapper.path("reviewerModel").asText("UNKNOWN");
            JsonNode analysis = wrapper.has("analysis") ? wrapper.path("analysis") : wrapper;
            JsonNode suggestions = analysis.path("suggestedImprovements");
            if (!suggestions.isArray()) continue;
            for (int i = 0; i < suggestions.size(); i++) {
                JsonNode suggestion = suggestions.get(i);
                if (!suggestion.isTextual() || suggestion.asText().isBlank()) continue;
                proposals.add(item("origin", model, "sourcePath", "/reviews/" + model + "/suggestedImprovements/" + i,
                        "proposal", suggestion.asText(), "evidenceStatus", "NOT_VERIFIED",
                        "reviewStatus", "REQUIRES_QA_MANAGER_REVIEW"));
            }
        }
        return proposals;
    }

    private static boolean containsRiskDimension(JsonNode risks, String dimension) {
        for (JsonNode r : risks) if (dimension.equalsIgnoreCase(r.path("dimension").asText(""))) return true;
        return false;
    }
    private static List<String> dataGaps(JsonNode source) {
        List<String> gaps = new ArrayList<>();
        if (source.path("totalBugs").asInt(0)>0) gaps.add("Defect severity, status, affected components and business impact are not supplied in aggregate metrics");
        if (source.path("uncoveredRequirements").asInt(0)>0) gaps.add("Identity and criticality of uncovered requirements are not supplied in aggregate metrics");
        if (source.path("codeSmells").asInt(0)>0) gaps.add("Code-smell hotspot locations and links to defects are not supplied");
        if (source.has("confidence")) gaps.add("Meaning and calculation method of confidence must be documented before using it as evidence of reliability");
        return gaps;
    }
    private static Map<String,Object> action(int priority, String category, String action, String evidence, String done) {
        return item("priority", priority, "category", category, "action", action, "evidence", evidence, "acceptanceCriterion", done, "origin", "DETERMINISTIC_QA_PLAYBOOK");
    }
    private static Map<String,Object> item(Object... values) {
        Map<String,Object> result = new LinkedHashMap<>();
        for (int i=0; i<values.length; i+=2) result.put((String)values[i], values[i+1]);
        return result;
    }
}
