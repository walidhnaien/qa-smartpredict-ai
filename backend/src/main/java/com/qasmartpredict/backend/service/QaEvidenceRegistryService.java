package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class QaEvidenceRegistryService {
    private final ObjectMapper mapper;
    private final QaAgentSkillsService skills;
    public QaEvidenceRegistryService(ObjectMapper mapper, QaAgentSkillsService skills) { this.mapper=mapper; this.skills=skills; }
    public ObjectNode registry(JsonNode official) {
        ObjectNode result=mapper.createObjectNode();
        JsonNode definitions=skills.metricsCatalog().path("metrics");
        definitions.fields().forEachRemaining(e -> {
            JsonNode value=official.get(e.getKey());
            if(value!=null && value.isValueNode() && !value.isNull()) {
                ObjectNode item=result.putObject(e.getKey());
                item.set("value",value);
                for(String key:List.of("unit","interpretation","limitations"))
                    if(e.getValue().hasNonNull(key)) item.set(key,e.getValue().get(key));
            }
        });
        if(result.isEmpty()) throw new IllegalStateException("No catalog evidence available");
        return result;
    }
    public ObjectNode guidedRegistry(JsonNode official, JsonNode checks, String role) {
        ObjectNode result=mapper.createObjectNode();
        int index=0;
        for(JsonNode check:checks) {
            String type=check.path("type").asText();
            String id="I"+(++index);
            if(!Set.of("RISK_LEVEL_CONTRADICTION","METRIC_SEMANTICS_AMBIGUOUS").contains(type)) continue;
            ObjectNode item=result.putObject(id);
            item.set("issue",check);
            item.put("required", "QWEN".equals(role));
            item.putArray("actions").add(type.equals("RISK_LEVEL_CONTRADICTION")?"CORRECT_RISK_LEVEL":"CLARIFY_METRIC");
        }
        if("GEMMA".equals(role)) {
            addOption(result,official,"totalBugs","CHECK_SEVERITY","INVESTIGATE_CAUSE");
            addOption(result,official,"uncoveredRequirements","CHECK_COVERAGE");
            addOption(result,official,"codeSmells","INVESTIGATE_CAUSE");
            addOption(result,official,"releaseDecision","EXPLAIN_RISK");
        } else {
            addOption(result,official,"confidence","EXPLAIN_CONFIDENCE");
            addOption(result,official,"bugRatio","CLARIFY_METRIC");
        }
        return result;
    }
    private void addOption(ObjectNode result,JsonNode official,String key,String... actions) {
        if(!official.hasNonNull(key)) return;
        if(official.get(key).isNumber() && official.get(key).asDouble()==0) return;
        ObjectNode item=result.putObject(key); item.set("officialValue",official.get(key));
        item.put("required",false); var a=item.putArray("actions"); for(String action:actions)a.add(action);
    }
    public Map<String,Object> guidedSchema(JsonNode registry,String role) {
        List<String> options=new ArrayList<>(); int required=0;
        var it=registry.fields(); while(it.hasNext()) {var e=it.next(); if(e.getValue().path("required").asBoolean())required++; for(JsonNode a:e.getValue().path("actions"))options.add(e.getKey()+":"+a.asText());}
        int min=Math.max(required, "GEMMA".equals(role)&&!options.isEmpty()?1:0);
        return Map.of("type","object","additionalProperties",false,"required",List.of("selections"),"properties",Map.of("selections",Map.of("type","array","minItems",min,"maxItems",required+2,"items",Map.of("type","string","enum",options))));
    }
    public ObjectNode validateGuided(JsonNode raw,JsonNode registry,String role) {
        if(!raw.isObject()||raw.size()!=1||!raw.path("selections").isArray())throw new IllegalStateException("Invalid guided structure");
        ObjectNode result=mapper.createObjectNode(); var findings=result.putArray("findings");
        for(String field:List.of("strengths","weaknesses","missingRisks","unsupportedClaims","questionsForQaManager","suggestedImprovements"))result.putArray(field);
        Set<String> seen=new HashSet<>(), covered=new HashSet<>(); int required=0;
        var it=registry.fields(); while(it.hasNext())if(it.next().getValue().path("required").asBoolean())required++;
        if(raw.path("selections").size()>required+2)throw new IllegalStateException("Too many selections");
        for(JsonNode selection:raw.path("selections")) {
            if(!selection.isTextual()||!seen.add(selection.asText()))throw new IllegalStateException("Invalid or duplicate selection");
            String[] pair=selection.asText().split(":",-1); if(pair.length!=2||!registry.has(pair[0]))throw new IllegalStateException("Unknown selection");
            JsonNode item=registry.get(pair[0]); boolean allowed=false; for(JsonNode a:item.path("actions"))if(a.asText().equals(pair[1]))allowed=true;
            if(!allowed)throw new IllegalStateException("Incompatible action");
            if(item.path("required").asBoolean())covered.add(pair[0]);
            ObjectNode finding=findings.addObject(); finding.put("evidenceId",pair[0]); finding.put("action",pair[1]); finding.set("officialEvidence",item);
            result.withArray("suggestedImprovements").add(pair[0]+": "+(pair[1].equals("CORRECT_RISK_LEVEL")?"Correct the candidate risk level to the official value.":ACTIONS.get(pair[1])));
        }
        if(covered.size()!=required)throw new IllegalStateException("Mandatory issue coverage incomplete: "+covered.size()+"/"+required);
        if("GEMMA".equals(role)&&!registry.isEmpty()&&seen.isEmpty())throw new IllegalStateException("No risk investigation selected");
        result.put("requiredIssues",required); result.put("coveredIssues",covered.size()); if(required>0)result.put("issueCoverage",100);else result.putNull("issueCoverage");
        result.put("verdict",seen.isEmpty()?"PASS":"NEEDS_IMPROVEMENT"); result.put("reasoning","Java validated selections and mandatory coverage; relevance and actual corrections require review."); return result;
    }
    private static final Map<String,String> ACTIONS=Map.of(
        "CLARIFY_METRIC","Clarify the metric using its official definition.",
        "CHECK_SEVERITY","Obtain defect severity and impact evidence.",
        "INVESTIGATE_CAUSE","Investigate root causes before proposing fixes.",
        "CHECK_COVERAGE","Investigate uncovered scope and its impact.",
        "EXPLAIN_RISK","Explain the official risk and residual uncertainty.",
        "EXPLAIN_CONFIDENCE","Explain confidence without treating it as release safety.");
    private static final Map<String,Set<String>> ALLOWED=Map.of(
        "CHECK_SEVERITY",Set.of("bugRatio","defectScore","totalBugs","bugs"),
        "CHECK_COVERAGE",Set.of("epicCoverage","epicCoverageScore","requirementCoverage","codeCoverage","sonarCoverage","coverageScore"),
        "EXPLAIN_RISK",Set.of("riskLevel","releaseDecision"),
        "EXPLAIN_CONFIDENCE",Set.of("confidence"));
    public Map<String,Object> schema(JsonNode registry) {
        List<String> ids=new ArrayList<>(); registry.fieldNames().forEachRemaining(ids::add);
        Map<String,Object> fields=Map.of("evidenceId",Map.of("type","string","enum",ids),"action",Map.of("type","string","enum",new TreeSet<>(ACTIONS.keySet())));
        return Map.of("type","object","additionalProperties",false,"required",List.of("verdict","findings"),"properties",Map.of(
            "verdict",Map.of("type","string","enum",List.of("PASS","NEEDS_IMPROVEMENT","FAIL")),
            "findings",Map.of("type","array","maxItems",2,"items",Map.of("type","object","additionalProperties",false,"required",List.of("evidenceId","action"),"properties",fields))));
    }
    public ObjectNode validate(JsonNode raw, JsonNode registry) {
        if(!raw.isObject() || raw.size()!=2 || !raw.path("verdict").isTextual() || !raw.path("findings").isArray()) throw new IllegalStateException("Invalid evidence review structure");
        String verdict=raw.path("verdict").asText(); JsonNode findings=raw.path("findings");
        if(!Set.of("PASS","NEEDS_IMPROVEMENT","FAIL").contains(verdict) || findings.size()>2 || ("PASS".equals(verdict)!=findings.isEmpty())) throw new IllegalStateException("Invalid verdict/findings combination");
        ObjectNode result=mapper.createObjectNode(); result.put("verdict",verdict);
        for(String field:List.of("strengths","weaknesses","missingRisks","unsupportedClaims","questionsForQaManager","suggestedImprovements")) result.putArray(field);
        var enriched=result.putArray("findings"); Set<String> seen=new HashSet<>();
        for(JsonNode f:findings) {
            if(!f.isObject() || f.size()!=2 || !f.path("evidenceId").isTextual() || !f.path("action").isTextual()) throw new IllegalStateException("Invalid finding");
            String id=f.path("evidenceId").asText(), action=f.path("action").asText();
            if(!registry.has(id) || !ACTIONS.containsKey(action) || (ALLOWED.containsKey(action) && !ALLOWED.get(action).contains(id)) || !seen.add(id+":"+action)) throw new IllegalStateException("Invalid evidence/action pair: "+id+":"+action);
            ObjectNode item=enriched.addObject(); item.put("evidenceId",id); item.put("action",action); item.set("officialEvidence",registry.get(id));
            result.withArray("suggestedImprovements").add(id+": "+ACTIONS.get(action));
        }
        result.put("reasoning","Advisory action selection; evidence references checked by Java, relevance requires human review.");
        return result;
    }
}
