package com.qasmartpredict.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Read-only projection of the same evidence batch used by revision. */
@RestController
@RequestMapping("/api/ai/multi-agent")
public class AiMultiAgentConsolidationController {
    private final AiMultiAgentEvidenceController evidence;
    private final ObjectMapper mapper;
    public AiMultiAgentConsolidationController(AiMultiAgentEvidenceController evidence, ObjectMapper mapper) {
        this.evidence=evidence; this.mapper=mapper;
    }
    @GetMapping("/consolidation/{validationId}")
    public Map<String,Object> consolidate(@PathVariable UUID validationId) throws Exception {
        Map<String,Object> result=new LinkedHashMap<>(evidence.intelligence(validationId));
        JsonNode reviews=mapper.valueToTree(result.get("reviews"));
        JsonNode intelligence=mapper.valueToTree(result.get("intelligence"));
        List<Map<String,Object>> list=new ArrayList<>();
        Set<String> verdicts=new HashSet<>();
        reviews.fields().forEachRemaining(e -> {
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("model",e.getKey()); row.put("role",e.getKey().startsWith("gemma")?"CRITIC":"COUNTER_ANALYST");
            row.put("executionStatus","SUCCESS"); row.put("analysis",e.getValue());
            String verdict=e.getValue().path("verdict").asText(); verdicts.add(verdict); row.put("verdict",verdict);
            row.put("evidenceStatus","AI_OPINION_NOT_FACT_CERTIFIED"); list.add(row);
        });
        boolean complete=Boolean.TRUE.equals(result.get("complete")) && Boolean.TRUE.equals(result.get("sameBatchVerified"));
        result.put("reviews",list);
        result.put("verdictAgreement",!complete?"INCOMPLETE":verdicts.equals(Set.of("NEEDS_IMPROVEMENT"))?"BOTH_NEED_IMPROVEMENT":verdicts.size()==1?"SAME_VERDICT":"DIFFERENT_VERDICTS");
        for(String key:List.of("objectiveChecks","nliAssessments","discoveryFindings","prioritizedActions")) result.put(key,intelligence.path(key));
        result.put("semanticAgreement","NOT_AUTOMATICALLY_VERIFIED");
        result.put("semanticQualityCertified",false);
        result.put("note","Latest evidence batch; only successful reviews included. Human review required.");
        return result;
    }
}
