package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

/** Evidence-aware triage of agent proposals. It never certifies semantic truth. */
@Service
public class AiAgentRecommendationService {
    private record Topic(String code, Pattern pattern, String metric, String evidence, String nextStep) {}
    private static Topic topic(String code, String regex, String metric, String evidence, String nextStep) {
        return new Topic(code, Pattern.compile(regex, Pattern.CASE_INSENSITIVE), metric, evidence, nextStep);
    }
    private static final List<Topic> TOPICS = List.of(
        topic("DEFECT_TRIAGE", "\\b(defects?|bugs?|severity|root cause|defect quality)\\b", "totalBugs", "Recorded defects require severity and impact assessment", "Review defect IDs, severity, status, ownership and release blockers"),
        topic("REQUIREMENT_GAP", "\\b(requirements?|coverage|traceability|acceptance tests?)\\b", "uncoveredRequirements", "Uncovered requirements require criticality assessment", "Identify uncovered requirement and business impact; link missing tests"),
        topic("SONAR_HOTSPOTS", "\\b(code smells?|technical debt|sonar|maintainability)\\b", "codeSmells", "Sonar issues warrant hotspot investigation", "Review Sonar locations and correlate with defects only if linked data exists"),
        topic("RELEASE_MITIGATION", "\\b(mitigation|release|rollback|go.with.risk|risk ranking|risk priorit)\\b", "releaseDecision", "Release decision requires documented risk acceptance", "Document residual risk, mitigation owner and rollback criteria"),
        topic("TEST_STRATEGY", "\\b(testing|tests?|qa processes|regression)\\b", "coverageScore", "Testing recommendations require test-level evidence", "Identify specific test gaps, scenarios and measurable exit criteria")
    );

    public Map<String,Object> evaluate(JsonNode source, List<Map<String,Object>> proposals) {
        Map<String,Map<String,Object>> groups = new LinkedHashMap<>();
        List<Map<String,Object>> unclassified = new ArrayList<>();
        for (Map<String,Object> proposal : proposals) {
            String text = Objects.toString(proposal.get("proposal"), "");
            List<Topic> matches = TOPICS.stream().filter(t -> t.pattern.matcher(text).find()).toList();
            if (matches.isEmpty()) {
                unclassified.add(Map.of("origin", Objects.toString(proposal.get("origin"), "UNKNOWN"),
                    "sourcePath", Objects.toString(proposal.get("sourcePath"), ""), "proposal", text,
                    "status", "REQUIRES_HUMAN_CLASSIFICATION"));
                continue;
            }
            for (Topic topic : matches) {
                Map<String,Object> group = groups.computeIfAbsent(topic.code, k -> {
                    Map<String,Object> g = new LinkedHashMap<>();
                    g.put("topic", topic.code);
                    g.put("recommendedInvestigation", topic.nextStep);
                    g.put("contributions", new ArrayList<Map<String,Object>>());
                    g.put("origins", new LinkedHashSet<String>());
                    return g;
                });
                @SuppressWarnings("unchecked") List<Map<String,Object>> contributions = (List<Map<String,Object>>)group.get("contributions");
                Map<String,Object> contribution = new LinkedHashMap<>(proposal);
                contribution.put("semanticVerification", "NOT_VERIFIED");
                contributions.add(contribution);
                @SuppressWarnings("unchecked") Set<String> origins = (Set<String>)group.get("origins");
                origins.add(Objects.toString(proposal.get("origin"), "UNKNOWN"));
            }
        }
        List<Map<String,Object>> ranked = new ArrayList<>();
        for (Topic t : TOPICS) {
            Map<String,Object> group = groups.get(t.code);
            if (group == null) continue;
            boolean metricPresent = source.hasNonNull(t.metric);
            boolean relevant = switch (t.code) {
                case "DEFECT_TRIAGE" -> source.path("totalBugs").asInt(0) > 0;
                case "REQUIREMENT_GAP" -> source.path("uncoveredRequirements").asInt(0) > 0;
                case "SONAR_HOTSPOTS" -> source.path("codeSmells").asInt(0) > 0;
                case "RELEASE_MITIGATION" -> "GO_WITH_RISK".equalsIgnoreCase(source.path("releaseDecision").asText());
                default -> metricPresent;
            };
            String evidence = metricPresent ? t.metric + "=" + source.path(t.metric).asText() : "Metric not supplied";
            @SuppressWarnings("unchecked") Set<String> origins = (Set<String>)group.get("origins");
            int priority = relevant && (t.code.equals("DEFECT_TRIAGE") || t.code.equals("REQUIREMENT_GAP") || t.code.equals("RELEASE_MITIGATION")) ? 1 : 2;
            group.put("priority", priority);
            group.put("officialEvidence", evidence);
            group.put("evidenceStatus", relevant ? "METRIC_SUPPORTED_INVESTIGATION" : "CONTEXT_NOT_ESTABLISHED");
            group.put("agreement", origins.size() > 1 ? "MULTIPLE_AGENTS_PROPOSE_TOPIC" : "SINGLE_AGENT_TOPIC");
            group.put("semanticVerification", "NOT_VERIFIED");
            group.put("requiresHumanDecision", true);
            group.put("origins", new ArrayList<>(origins));
            ranked.add(group);
        }
        ranked.sort(Comparator.comparingInt(g -> (int)g.get("priority")));
        return Map.of("agentDerivedInvestigations", ranked, "unclassifiedProposals", unclassified,
            "method", "Transparent topic grouping and metric grounding; no LLM consensus certification",
            "limitations", List.of("Keyword grouping may miss paraphrases or group unrelated recommendations",
                "A metric supports investigation, not the truth of an agent's causal claim",
                "Multi-agent agreement does not establish correctness"));
    }
}
