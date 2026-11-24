package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qasmartpredict.backend.controller.AiMultiAgentEvidenceController;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.TimeUnit;

/** Revises a persisted Llama V1 using independent reviewers and deterministic evidence checks. */
@Service
public class AiMultiAgentRevisionService {
    private final AiMultiAgentEvidenceController evidenceController;
    private final LlmGatewayService gateway;
    private final AiRevisionValidationService validator;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final QaMetricsCatalogService metricsCatalog;
    private final QaAgentSkillsService agentSkills;

    public AiMultiAgentRevisionService(AiMultiAgentEvidenceController evidenceController,
                                       LlmGatewayService gateway,
                                       AiRevisionValidationService validator,
                                       JdbcTemplate jdbc, ObjectMapper mapper,
                                       QaMetricsCatalogService metricsCatalog, QaAgentSkillsService agentSkills) {
        this.evidenceController = evidenceController;
        this.gateway = gateway;
        this.validator = validator;
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.metricsCatalog = metricsCatalog;
        this.agentSkills = agentSkills;
    }

    public Map<String,Object> revise(UUID validationId) throws Exception {
        Map<String,Object> evidence = evidenceController.intelligence(validationId);
        if (!Boolean.TRUE.equals(evidence.get("complete")) || !Boolean.TRUE.equals(evidence.get("sameBatchVerified")))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Both Gemma and Qwen successful reviews are required before revision");
        JsonNode official = mapper.valueToTree(evidence.get("officialMetrics"));
        JsonNode v1 = mapper.valueToTree(evidence.get("candidateAnalysis"));
        JsonNode reviews = mapper.valueToTree(evidence.get("reviews"));
        JsonNode intelligence = mapper.valueToTree(evidence.get("intelligence"));
        // Remove the large original prompt and redundant playbook fields from the LLM context.
        ObjectNode compactOfficial = agentSkills.compactOfficial(official);
        ObjectNode context = mapper.createObjectNode();
        context.set("officialMetrics", compactOfficial);
        context.set("qaMetricsCatalog", agentSkills.relevantCatalog(official));
        context.put("agentPolicyVersion", agentSkills.policyVersion());
        context.put("metricsCatalogVersion", agentSkills.catalogVersion());
        context.set("llamaV1", v1);
        context.set("gemmaAndQwenReviews", reviews);
        ObjectNode checks = mapper.createObjectNode();
        for (String key : List.of("objectiveChecks", "reviewerChecks", "dataGaps", "agentRecommendationAssessment")) {
            JsonNode value = intelligence.path(key);
            if (key.equals("agentRecommendationAssessment")) {
                // Grouped proposals can be very large; model already receives the full independent reviews.
                ObjectNode limited = mapper.createObjectNode();
                limited.set("limitations", value.path("limitations"));
                checks.set(key, limited);
            } else checks.set(key, value);
        }
        context.set("deterministicEvidenceChecks", checks);
        context.put("reviewPairBatchVerified", true);
        context.put("reviewBatchId", String.valueOf(evidence.get("batchId")));

        String systemPrompt = """
            You are Llama, a senior QA release analysis revision agent.
            Reconstruct the QA analysis from authoritative Java metrics and the
            QA metrics catalog; critically evaluate Gemma and Qwen critiques.
            The original Llama V1 is a fallible candidate, not factual authority.
            The catalog defines metric semantics; Java officialMetrics defines values.
            Never invent defect severity, business impact, root causes or evidence.
            Do not equate a quality score with a defect percentage or bug ratio.
            A confidence indicator is not proof of reliability or release safety.
            Preserve the official riskLevel and releaseDecision exactly.
            Review critiques individually, rejecting unsupported claims.
            Make recommendations actionable, evidenced and auditable.
            Declare only corrections visibly made in the revised output.

            Output ONLY one complete JSON object with exactly these top-level keys:
            executiveSummary (string), releaseRationale (string),
            topRisks (array of {dimension, severity, reason}),
            priorityActions (array of {priority, action, expectedImpact,
            evidence, origin, acceptanceCriterion}), managerMessage (string),
            confidenceExplanation (string), revision (object with arrays
            correctedIssues, acceptedCritiques, rejectedCritiques,
            unverifiedClaims, residualRisks, requiredEvidence).
            All revision arrays must exist, even if empty. At most two risks and two actions; short strings. No Markdown.
            """;
        systemPrompt = agentSkills.systemFor("LLAMA_REVISION", systemPrompt);
        String userPrompt = """
            Reconstruct this QA analysis. Use officialMetrics for factual values,
            qaMetricsCatalog for formulas and interpretations, llamaV1 only as
            the candidate to correct, and Gemma/Qwen as independent critics.
            Evaluate deterministicEvidenceChecks. Do not copy erroneous V1 claims.
            ACTUAL CONTEXT:
            """ + mapper.writeValueAsString(context) + """

            FINAL CHECK: do not confuse bugRatio with defectScore; do not claim
            confidence proves reliability; do not contradict official riskLevel;
            include the complete revision object and verifiable action criteria.
            """;
        UUID revisionId = UUID.randomUUID();
        long started = System.nanoTime();
        String raw = null, status = "ERROR", error = null;
        JsonNode revised = null;
        Map<String,Object> validation = null;
        try {
            raw = gateway.generate(systemPrompt, userPrompt);
            revised = mapper.readTree(raw);
            validation = validator.validate(official, v1, revised);
            status = "SUCCESS";
        } catch (Exception ex) {
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        // Append-only history: preserve failed attempts and raw output for reproducibility.
        jdbc.update("""
            INSERT INTO ai_multi_agent_revision
              (id, validation_id, revision_version, model, prompt_version, input_snapshot,
               system_prompt, user_prompt, raw_response, revised_analysis, validation_report,
               execution_status, error_message, duration_ms)
            VALUES (?, ?, 'LLAMA_V2', ?, 'REVISION_AGENT_SKILLS_V2', ?::jsonb,
                    ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)
            """, revisionId, validationId, gateway.getModel(), mapper.writeValueAsString(context),
                systemPrompt, userPrompt, raw,
                revised == null ? null : mapper.writeValueAsString(revised),
                validation == null ? null : mapper.writeValueAsString(validation),
                status, error, durationMs);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("revisionId", revisionId);
        result.put("validationId", validationId);
        result.put("model", gateway.getModel());
        result.put("executionStatus", status);
        result.put("durationMs", durationMs);
        result.put("officialDecision", official.path("releaseDecision").asText());
        result.put("sameBatchVerified", true);
        result.put("revisedAnalysis", revised);
        result.put("validationReport", validation);
        result.put("error", error);
        return result;
    }

    public List<Map<String,Object>> history(UUID validationId) {
        return jdbc.queryForList("""
            SELECT id AS "revisionId", validation_id AS "validationId", model,
                   execution_status AS "executionStatus", duration_ms AS "durationMs",
                   revised_analysis::text AS "revisedAnalysisJson",
                   validation_report::text AS "validationReportJson",
                   error_message AS "error", created_at AS "createdAt"
            FROM ai_multi_agent_revision WHERE validation_id=?
            ORDER BY created_at DESC, id DESC
            """, validationId);
    }
}
