package com.qasmartpredict.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/** Read-only consolidation of persisted independent Ollama reviews. */
@RestController
@RequestMapping("/api/ai/multi-agent")
public class AiMultiAgentConsolidationController {
    private static final Pattern MODERATE_RISK = Pattern.compile("(?i)\\b(?:risk|main risk)[^.!?]{0,100}\\bmoderate\\b|\\bmoderate\\b[^.!?]{0,60}\\brisk\\b");
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public AiMultiAgentConsolidationController(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @GetMapping("/consolidation/{validationId}")
    public Map<String, Object> consolidate(@PathVariable UUID validationId) throws Exception {
        List<Map<String, Object>> sourceRows = jdbc.queryForList("""
            SELECT source_metrics::text AS source, llama_response::text AS candidate
            FROM ai_validation_result WHERE id = ?
            """, validationId);
        if (sourceRows.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Unknown validationId");
        }
        JsonNode source = mapper.readTree((String) sourceRows.get(0).get("source"));
        JsonNode candidate = mapper.readTree((String) sourceRows.get(0).get("candidate"));

        // Latest review per fixed model, not a claim that these runs share one batch.
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT DISTINCT ON (reviewer_model)
                id::text AS review_id, reviewer_model, execution_status, review_status,
                parsed_response::text AS analysis, created_at
            FROM ai_review_result
            WHERE validation_id = ? AND prompt_version = 'MULTI_AGENT_V1'
              AND reviewer_model IN ('gemma3:4b', 'qwen2.5:3b')
            ORDER BY reviewer_model, created_at DESC, id DESC
            """, validationId);

        List<Map<String, Object>> reviews = new ArrayList<>();
        List<String> allQuestions = new ArrayList<>();
        List<String> allImprovements = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String model = (String) row.get("reviewer_model");
            JsonNode analysis = row.get("analysis") == null ? null : mapper.readTree((String) row.get("analysis"));
            Map<String, Object> review = new LinkedHashMap<>();
            review.put("reviewId", row.get("review_id"));
            review.put("model", model);
            review.put("role", model.startsWith("gemma") ? "CRITIC" : "COUNTER_ANALYST");
            review.put("executionStatus", row.get("execution_status"));
            review.put("verdict", row.get("review_status"));
            review.put("createdAt", row.get("created_at"));
            review.put("analysis", analysis);
            review.put("evidenceStatus", "AI_OPINION_NOT_FACT_CERTIFIED");
            reviews.add(review);
            if ("SUCCESS".equals(row.get("execution_status")) && analysis != null) {
                addStrings(allQuestions, analysis.path("questionsForQaManager"));
                addStrings(allImprovements, analysis.path("suggestedImprovements"));
            }
        }

        List<Map<String, Object>> objectiveChecks = new ArrayList<>();
        String officialRisk = source.path("riskLevel").asText("");
        if ("HIGH".equalsIgnoreCase(officialRisk) || "CRITICAL".equalsIgnoreCase(officialRisk)) {
            // Flag for human review; do not claim every mention of "moderate" is contradictory.
            for (String field : List.of("executiveSummary", "releaseRationale", "managerMessage")) {
                String value = candidate.path(field).asText("");
                if (MODERATE_RISK.matcher(value).find()) {
                    Map<String, Object> check = new LinkedHashMap<>();
                    check.put("type", "POSSIBLE_RISK_LEVEL_CONTRADICTION");
                    check.put("candidateField", field);
                    check.put("officialRisk", officialRisk);
                    check.put("candidateText", value);
                    check.put("status", "REQUIRES_REVIEW");
                    objectiveChecks.add(check);
                }
            }
        }

        boolean bothSuccessful = reviews.size() == 2 && reviews.stream()
            .allMatch(r -> "SUCCESS".equals(r.get("executionStatus")));
        boolean bothNeedsImprovement = bothSuccessful && reviews.stream()
            .allMatch(r -> "NEEDS_IMPROVEMENT".equals(r.get("verdict")));
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("validationId", validationId);
        output.put("officialMetrics", source);
        output.put("candidateAnalysis", candidate);
        output.put("reviews", reviews);
        output.put("complete", bothSuccessful);
        output.put("verdictAgreement", bothNeedsImprovement ? "BOTH_NEED_IMPROVEMENT"
            : bothSuccessful && reviews.get(0).get("verdict").equals(reviews.get(1).get("verdict"))
                ? "SAME_VERDICT" : bothSuccessful ? "DIFFERENT_VERDICTS" : "INCOMPLETE");
        output.put("objectiveChecks", objectiveChecks);
        output.put("suggestedImprovements", allImprovements);
        output.put("questionsForQaManager", allQuestions);
        output.put("semanticAgreement", "NOT_AUTOMATICALLY_VERIFIED");
        output.put("note", "Latest independent review per model. No semantic equivalence, factual certification, or shared batch is implied.");
        return output;
    }

    private static void addStrings(List<String> target, JsonNode array) {
        if (!array.isArray()) return;
        for (JsonNode item : array) {
            if (item.isTextual() && !item.asText().isBlank() && !target.contains(item.asText())) {
                target.add(item.asText());
            }
        }
    }
}
