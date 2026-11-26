package com.qasmartpredict.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasmartpredict.backend.service.QaStatementRegistryService;
import com.qasmartpredict.backend.service.QaNliValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import com.qasmartpredict.backend.service.QaAgentSkillsService;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ai/multi-agent")
public class LocalMultiAgentReviewController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final com.qasmartpredict.backend.service.OllamaLifecycleService lifecycle;
    private final String gemmaModel;
    private final String qwenModel;
    private final QaAgentSkillsService agentSkills;
    private final com.qasmartpredict.backend.service.AiEvidenceIntelligenceService intelligence;
    private final com.qasmartpredict.backend.service.QaEvidenceRegistryService semantic;

    public LocalMultiAgentReviewController(
            JdbcTemplate jdbc, ObjectMapper mapper,
            com.qasmartpredict.backend.service.OllamaLifecycleService lifecycle,
            @Value("${qa.llm.gemma.model:gemma3-qa:4b}") String gemmaModel,
            @Value("${qa.llm.qwen.model:qwen2.5:3b}") String qwenModel,
            QaAgentSkillsService agentSkills, com.qasmartpredict.backend.service.AiEvidenceIntelligenceService intelligence, com.qasmartpredict.backend.service.QaEvidenceRegistryService semantic) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.gemmaModel = gemmaModel;
        this.qwenModel = qwenModel;
        this.agentSkills = agentSkills;
        this.intelligence = intelligence;
        this.semantic = semantic;
        this.lifecycle = lifecycle;
    }

    @PostMapping("/review/{validationId}")
    public Map<String, Object> review(@PathVariable UUID validationId) {
        Map<String, Object> original = jdbc.queryForMap("""
                SELECT source_metrics::text AS source_metrics,
                       llama_response::text AS llama_response
                FROM ai_validation_result WHERE id = ?
                """, validationId);
        String source = (String) original.get("source_metrics");
        String candidate = (String) original.get("llama_response");
        UUID batchId = UUID.randomUUID();
        List<Map<String, Object>> results = new ArrayList<>();
        results.add(runOne(validationId, batchId, "GEMMA", gemmaModel, source, candidate));
        results.add(runOne(validationId, batchId, "QWEN", qwenModel, source, candidate));
        return Map.of("validationId", validationId, "batchId", batchId,
                "reviews", results,
                "note", "Independent advisory reviews; disagreements are retained. No automatic factual certification.");
    }

    @GetMapping("/review/{validationId}")
    public List<Map<String, Object>> history(@PathVariable UUID validationId) {
        return jdbc.queryForList("""
                SELECT id::text AS id, reviewer_model, reviewer_provider,
                       prompt_version, execution_status, review_status,
                       parsed_response::text AS parsed_response,
                       raw_response, error_message, total_duration_ms, prompt_tokens, completion_tokens, load_duration_ms, prompt_eval_duration_ms, eval_duration_ms,
                       response_metadata->>'done_reason' AS done_reason, request_payload->>'reviewBatchId' AS batch_id,
                       created_at
                FROM ai_review_result
                WHERE validation_id = ? AND prompt_version IN ('MULTI_AGENT_V1','MULTI_AGENT_SKILLS_V1','MULTI_AGENT_SKILLS_V2','MULTI_AGENT_SKILLS_V2_1','MULTI_AGENT_EVIDENCE_V3','MULTI_AGENT_EVIDENCE_V3_1','MULTI_AGENT_EVIDENCE_V3_3','MULTI_AGENT_EVIDENCE_V3_4','MULTI_AGENT_EVIDENCE_V3_4_1','MULTI_AGENT_EVIDENCE_V3_4_2','MULTI_AGENT_EVIDENCE_V3_4_3','MULTI_AGENT_NLI_V3_4_4','MULTI_AGENT_NLI_V3_4_5','MULTI_AGENT_NLI_V3_4_6')
                ORDER BY created_at DESC
                """, validationId);
    }

    public Map<String,Object> reviewRole(UUID validationId, UUID batchId, String role) {
        var row = jdbc.queryForMap("SELECT source_metrics::text AS source, llama_response::text AS candidate FROM ai_validation_result WHERE id=?", validationId);
        String model = switch(role) { case "GEMMA" -> gemmaModel; case "QWEN" -> qwenModel; default -> throw new IllegalArgumentException("Unknown role"); };
        return runOne(validationId,batchId,role,model,(String)row.get("source"),(String)row.get("candidate"));
    }

    private JsonNode readSourceMetrics(String source) {
        try { return mapper.readTree(source); }
        catch (Exception ex) { return null; }
    }

    private Map<String, Object> runOne(UUID validationId, UUID batchId,
                                      String role, String model,
                                      String source, String candidate) {
        UUID reviewId = com.qasmartpredict.backend.service.orchestrator.WorkflowArtifactId.next();
        long start = System.nanoTime();
        String system = agentSkills.nliSystem(role);
        JsonNode official = agentSkills.compactOfficial(readSourceMetrics(source));
        JsonNode candidateNode = readSourceMetrics(candidate);
        JsonNode fullRegistry = QaStatementRegistryService.build(mapper,candidateNode);
        JsonNode statements = QaStatementRegistryService.forRole(mapper,fullRegistry,role);
        JsonNode pairs = QaNliValidationService.pairs(mapper,statements,official,agentSkills.metricsCatalog(),role);
        String user = QaNliValidationService.prompt(pairs);
        JsonNode requestPayload = null;
        String raw = null;
        JsonNode parsed = null;
        JsonNode metadata = null;
        String status = "ERROR";
        String verdict = null;
        String error = null;
        Long durationMs;
        Integer promptTokens = null, completionTokens = null;
        Long loadNs = null, promptNs = null, evalNs = null;
        String phase = "REQUEST";
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("model", model);
            request.put("stream", false);
            request.put("format", QaNliValidationService.schema(pairs));
            request.put("keep_alive", lifecycle.keepAlive());
            request.put("options", Map.of("temperature", 0.1, "num_ctx", 4096, "num_predict", 256,"repeat_penalty", 1.0));
            request.put("messages", List.of(Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)));
            requestPayload = mapper.valueToTree(request);
            if (pairs.isEmpty()) throw new IllegalStateException("No matched NLI pairs; review not performed");
            phase = "OLLAMA_CALL";
            String responseBody = lifecycle.chat(request, String.class);
            phase = "RESPONSE_PARSE";
            metadata = responseBody == null ? null : mapper.readTree(responseBody);
            if (metadata == null) throw new IllegalStateException("Empty Ollama response");
            raw = metadata.path("message").path("content").asText("");
            if (raw.isBlank()) throw new IllegalStateException("Empty judge output");
            if ("length".equals(metadata.path("done_reason").asText()))
                throw new IllegalStateException("Truncated output: token limit reached");
            parsed = mapper.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(raw);
            if (!parsed.isObject()) throw new IllegalStateException("Judge output is not a JSON object");
            phase = "DISCOVERY_VALIDATION";
            parsed = QaNliValidationService.validate(mapper, parsed, pairs);
            verdict = parsed.path("verdict").asText();
            status = "SUCCESS";
            phase = "COMPLETE";
        } catch (Exception ex) {
            error = phase + ": " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
        } finally {
            durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        }
        if (metadata != null) {
            if (metadata.path("prompt_eval_count").isIntegralNumber()) promptTokens = metadata.path("prompt_eval_count").asInt();
            if (metadata.path("eval_count").isIntegralNumber()) completionTokens = metadata.path("eval_count").asInt();
            if (metadata.path("load_duration").isIntegralNumber()) loadNs = metadata.path("load_duration").asLong();
            if (metadata.path("prompt_eval_duration").isIntegralNumber()) promptNs = metadata.path("prompt_eval_duration").asLong();
            if (metadata.path("eval_duration").isIntegralNumber()) evalNs = metadata.path("eval_duration").asLong();
        }
        if (metadata != null && metadata.isObject()) ((com.fasterxml.jackson.databind.node.ObjectNode) metadata).put("reviewBatchId", batchId.toString());
        if (requestPayload == null) requestPayload = mapper.createObjectNode();
        ((com.fasterxml.jackson.databind.node.ObjectNode) requestPayload).put("reviewBatchId", batchId.toString());
        ((com.fasterxml.jackson.databind.node.ObjectNode) requestPayload).put("reviewerRole",role).set("statementRegistry",statements);
        ((com.fasterxml.jackson.databind.node.ObjectNode) requestPayload).put("executionPhase",phase).set("matchedPairs",pairs);
        // Append-only: never update existing historical reviewer records.
        jdbc.update("""
                INSERT INTO ai_review_result (
                  id, validation_id, reviewer_model, reviewer_provider,
                  prompt_version, output_mode, system_prompt, user_prompt,
                  request_payload, raw_response, response_metadata, parsed_response,
                  review_status, validation_status, test_status,
                  prompt_tokens, completion_tokens, total_duration_ms,
                  load_duration_ms, prompt_eval_duration_ms, eval_duration_ms,
                  execution_status, error_message
                ) VALUES (?, ?, ?, 'OLLAMA', 'MULTI_AGENT_NLI_V3_4_6', 'JSON_SCHEMA', ?, ?,
                  CAST(? AS jsonb), ?, CAST(? AS jsonb), CAST(? AS jsonb),
                  ?, 'NOT_RUN', 'NOT_RUN', ?, ?, ?, ?, ?, ?, ?, ?)
                """, reviewId, validationId, model, system, user,
                requestPayload == null ? null : requestPayload.toString(),
                raw, metadata == null ? null : metadata.toString(),
                parsed == null ? null : parsed.toString(), verdict,
                promptTokens, completionTokens, durationMs,
                loadNs == null ? null : TimeUnit.NANOSECONDS.toMillis(loadNs),
                promptNs == null ? null : TimeUnit.NANOSECONDS.toMillis(promptNs),
                evalNs == null ? null : TimeUnit.NANOSECONDS.toMillis(evalNs),
                status, error);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reviewId", reviewId);
        result.put("role", role);
        result.put("model", model);
        result.put("executionStatus", status);
        result.put("verdict", verdict);
        result.put("analysis", parsed);
        result.put("durationMs", durationMs);
        result.put("error", error);
        result.put("promptTokens", promptTokens);
        result.put("completionTokens", completionTokens);
        result.put("statementRegistry",statements);
        result.put("matchedPairs",pairs);
        result.put("matchedClaimCount",pairs.size());
        result.put("eligibleClaimCount",com.qasmartpredict.backend.service.QaAtomicClaimService.extract(mapper,statements).size());
        java.util.Set<String> matchedFields = new java.util.HashSet<>();
        pairs.forEach(pair -> matchedFields.add(pair.path("parentStatementId").asText()));
        result.put("matchedFieldCount",matchedFields.size());
        result.put("eligibleFieldCount",statements.size());
        result.put("executionPhase", phase);
        result.put("loadDurationMs",loadNs == null ? null : TimeUnit.NANOSECONDS.toMillis(loadNs));
        result.put("promptEvalDurationMs",promptNs == null ? null : TimeUnit.NANOSECONDS.toMillis(promptNs));
        result.put("evalDurationMs",evalNs == null ? null : TimeUnit.NANOSECONDS.toMillis(evalNs));
        result.put("doneReason", metadata == null ? null : metadata.path("done_reason").asText(null));
        return result;
    }
}
