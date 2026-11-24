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
        ObjectNode context = mapper.createObjectNode();
        context.set("officialMetrics", agentSkills.compactOfficial(official));
        ObjectNode definitions = context.putObject("definitions");
        JsonNode catalog = agentSkills.metricsCatalog().path("metrics");
        for (String key : List.of("defectScore", "bugRatio", "confidence")) {
            JsonNode definition = catalog.path(key);
            if (definition.isObject()) {
                ObjectNode d = definitions.putObject(key);
                for (String field : List.of("formula", "unit", "interpretation"))
                    if (definition.has(field)) d.set(field, definition.get(field));
            }
        }
        ObjectNode corrections = context.putObject("corrections");
        for (JsonNode review : reviews) {
            for (JsonNode finding : review.path("findings")) {
                String id = finding.path("evidenceId").asText();
                String action = finding.path("action").asText();
                ObjectNode correction = mapper.createObjectNode();
                correction.put("action", action);
                JsonNode issue = finding.path("officialEvidence").path("issue");
                for (String field : List.of("field", "officialValue", "type"))
                    if (issue.has(field)) correction.set(field, issue.get(field));
                corrections.set(id + ":" + action, correction);
            }
        }
        context.set("objectiveChecks", intelligence.path("objectiveChecks"));
        context.set("qaActions", intelligence.path("prioritizedActions"));
        context.remove("corrections");
        context.put("reviewBatchId", String.valueOf(evidence.get("batchId")));
        String systemPrompt = agentSkills.systemFor("LLAMA_REVISION", """
            Suggest up to two investigative questions for the QA manager based on the evidence.
            Do not rewrite official facts, assign severity, or change the release decision.
            Questions only, no factual assertions. Keep each question under 20 words.
            Output only the schema object.
            """);
        String userPrompt = mapper.writeValueAsString(context);
        Map<String,Object> schema = objectSchema(Map.of("questions", Map.of("type","array",
                "items",Map.of("type","string","minLength",1),"maxItems",2)));
        // Audit snapshot retains the full original evidence, separate from the model prompt.
        ObjectNode audit = mapper.valueToTree(evidence);
        audit.set("compactContext", context);
        audit.set("outputSchema", mapper.valueToTree(schema));
        UUID revisionId = UUID.randomUUID();
        long started = System.nanoTime();
        String raw = null, status = "ERROR", error = null;
        JsonNode revised = null;
        Map<String,Object> validation = null;
        try {
            raw = gateway.generateRevision(systemPrompt, userPrompt, schema);
            JsonNode proposal = mapper.readTree(raw);
            if (!proposal.isObject() || proposal.size()!=1 || !proposal.path("questions").isArray()
                    || proposal.path("questions").size()>2) throw new IllegalStateException("Invalid question schema");
            for (JsonNode q : proposal.path("questions")) {
                if (!q.isTextual() || q.asText().isBlank() || !q.asText().trim().endsWith("?")
                        || q.asText().trim().split("\\s+").length>20)
                    throw new IllegalStateException("Invalid investigative question");
            }
            ObjectNode assembled = mapper.createObjectNode();
            String risk = official.path("riskLevel").asText();
            String main = official.path("mainRisk").asText();
            String decision = official.path("releaseDecision").asText();
            if (risk.isBlank() || main.isBlank() || decision.isBlank())
                throw new IllegalStateException("Missing official facts");
            String facts = "Official risk: " + risk + "; main risk: " + main + "; decision: " + decision + ".";
            String definition = "DefectScore is a quality score, not a defect ratio. BugRatio is bugs per 100 stories.";
            assembled.put("executiveSummary", facts + " " + definition);
            assembled.put("releaseRationale", facts + " Review residual risks before approval.");
            assembled.put("managerMessage", facts + " Review the evidence-backed actions with the QA manager.");
            assembled.put("confidenceExplanation", "Confidence in the analysis does not establish release safety. Official confidence: " + official.path("confidence").asText("unavailable") + ".");
            for (String key : List.of("riskLevel","mainRisk","releaseDecision","qis","confidence","defectScore","bugRatio"))
                if (official.has(key)) assembled.set(key,official.get(key));
            ObjectNode top = assembled.putArray("topRisks").addObject();
            top.put("dimension",main); top.put("severity",risk); top.put("reason",facts);
            var actions = assembled.putArray("priorityActions");
            for (JsonNode item : intelligence.path("prioritizedActions")) {
                ObjectNode action = actions.addObject();
                for (String key : List.of("priority","action","evidence","acceptanceCriterion"))
                    if (item.has(key)) action.set(key,item.get(key));
                action.put("origin","DETERMINISTIC_QA_PLAYBOOK");
                action.put("expectedImpact","Not quantified; requires QA review");
            }
            ObjectNode advisory = assembled.putObject("aiAdvisory");
            advisory.set("questions",proposal.get("questions"));
            advisory.put("status","HUMAN_REVIEW_REQUIRED");
            ObjectNode revision = assembled.putObject("revision");
            for (String key : List.of("correctedIssues", "acceptedCritiques", "rejectedCritiques", "unverifiedClaims", "residualRisks", "requiredEvidence"))
                revision.putArray(key);
            revised = assembled;
            validation = new LinkedHashMap<>(validator.validate(official, v1, revised));
            int total=0, resolved=0;
            var resolution = mapper.createArrayNode();
            for (JsonNode check : intelligence.path("objectiveChecks")) {
                String id = "I" + (++total);
                String type = check.path("type").asText();
                String field = check.path("field").asText();
                boolean fixed = false;
                if ("RISK_LEVEL_CONTRADICTION".equals(type))
                    fixed = List.of("executiveSummary","releaseRationale","managerMessage").contains(field)
                            && assembled.path(field).asText().startsWith(facts);
                else if ("METRIC_SEMANTICS_AMBIGUOUS".equals(type) && "defectScore".equals(field))
                    fixed = assembled.path("executiveSummary").asText().contains(definition)
                            && assembled.path("defectScore").equals(official.path("defectScore"));
                ObjectNode entry=resolution.addObject();
                entry.put("issueId",id); entry.put("type",type); entry.put("field",field);
                entry.put("resolved",fixed); entry.put("origin","JAVA_DETERMINISTIC_ASSEMBLY");
                if (fixed) { resolved++; ((com.fasterxml.jackson.databind.node.ArrayNode)revision.get("correctedIssues")).add(id); }
            }
            validation.put("issueResolution",resolution);
            validation.put("detectedIssues",total); validation.put("resolvedIssues",resolved);
            validation.put("issueResolutionRate",total==0 ? null : 100.0*resolved/total);
            validation.put("semanticQualityCertified",false);
            validation.put("humanReviewRequired",true);
            if (resolved<total || actions.isEmpty()) validation.put("status","REQUIRES_REVIEW");
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
            VALUES (?, ?, 'LLAMA_V2', ?, 'REVISION_AGENT_CONTROLLED_V3_1', ?::jsonb,
                    ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)
            """, revisionId, validationId, gateway.getModel(), mapper.writeValueAsString(audit),
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

    private static Map<String,Object> objectSchema(Map<String,Object> properties) {
        return Map.of("type", "object", "properties", properties,
                "required", new ArrayList<>(properties.keySet()), "additionalProperties", false);
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
