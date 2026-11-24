package com.qasmartpredict.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qasmartpredict.backend.service.AiEvidenceIntelligenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

/** Read-only evidence-backed intelligence from persisted Llama and local judge results. */
@RestController
@RequestMapping("/api/ai/multi-agent")
public class AiMultiAgentEvidenceController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final AiEvidenceIntelligenceService intelligence;
    public AiMultiAgentEvidenceController(JdbcTemplate jdbc, ObjectMapper mapper, AiEvidenceIntelligenceService intelligence) {
        this.jdbc=jdbc; this.mapper=mapper; this.intelligence=intelligence;
    }
    @GetMapping("/intelligence/{validationId}")
    public Map<String,Object> intelligence(@PathVariable UUID validationId) throws Exception {
        var rows=jdbc.queryForList("SELECT source_metrics::text AS source, llama_response::text AS candidate FROM ai_validation_result WHERE id=?",validationId);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Unknown validationId");
        JsonNode source=mapper.readTree((String)rows.get(0).get("source"));
        JsonNode candidate=mapper.readTree((String)rows.get(0).get("candidate"));
        var saved=jdbc.queryForList("""
          SELECT reviewer_model, execution_status, parsed_response::text AS analysis,
                 request_payload->>'reviewBatchId' AS batch_id
          FROM ai_review_result
          WHERE validation_id=? AND prompt_version='MULTI_AGENT_EVIDENCE_V3_3'
          ORDER BY created_at DESC, id DESC
          """,validationId);
        Map<String,JsonNode> byModel=new LinkedHashMap<>();
        Object batch = saved.isEmpty() ? null : saved.get(0).get("batch_id");
        for(var row:saved){
            if(batch != null && batch.equals(row.get("batch_id")) && "SUCCESS".equals(row.get("execution_status")) && row.get("analysis")!=null)
                byModel.putIfAbsent((String)row.get("reviewer_model"),mapper.readTree((String)row.get("analysis")));
        }
        List<JsonNode> reviews=new ArrayList<>();
        for(var entry:byModel.entrySet()) {
            var wrapper=mapper.createObjectNode();
            wrapper.put("reviewerModel",entry.getKey());
            wrapper.set("analysis",entry.getValue());
            reviews.add(wrapper);
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("validationId",validationId);
        result.put("officialMetrics",source);
        result.put("candidateAnalysis",candidate);
        result.put("reviews",byModel);
        result.put("complete",byModel.size()==2);
        result.put("sameBatchVerified",batch != null && byModel.size()==2);
        result.put("batchId",batch);
        result.put("intelligence",intelligence.assess(source,candidate,reviews));
        return result;
    }
}
