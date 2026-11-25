package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

/** Advisory NLI. Matching is heuristic; neither matching nor classification certifies truth. */
public final class QaNliValidationService {
    private static final List<String> RELATIONS=List.of("SUPPORTED","CONTRADICTED","NOT_ESTABLISHED");
    private QaNliValidationService() {}
    public static ObjectNode pairs(ObjectMapper m, JsonNode statements, JsonNode official, JsonNode catalog, String role) {
        ObjectNode out=m.createObjectNode();
        QaAtomicClaimService.extract(m,statements).fields().forEachRemaining(e -> {
            String text=e.getValue().path("hypothesis").asText();
            String t=text.toLowerCase(Locale.ROOT);
            String path=e.getValue().path("sourcePath").asText();
            String ref=null, premise=null;
            String type=e.getValue().path("claimType").asText();
            if(type.equals("RECOMMENDATION") || type.equals("HYPOTHESIS")) return;
            if(role.equals("GEMMA")) {
                if(t.contains("confidence")) { ref="G_CONFIDENCE"; premise="Confidence in analysis does not establish release safety or approval."; }
                else if(t.contains("approv") || t.contains("authoriz")) { ref="G_APPROVAL"; premise="A recommendation is not human approval. No human approval evidence is supplied in this analysis. Confidence does not establish approval."; }
                
                else if(t.contains("risk") || t.contains("release")) { ref="G_RISK"; premise="Official risk="+official.path("riskLevel").asText("UNKNOWN")+"; recommendation="+official.path("releaseDecision").asText("UNKNOWN")+". High risk is not a positive justification for release. Individual defect severity is not established by overall risk."; }
            } else {
                String key=null;
                if(path.equals("/executiveSummary") || path.equals("/releaseRationale") || path.equals("/managerMessage")) return;
                if(t.contains("epic") && (t.contains("requirement") || t.contains("risk"))) { ref="M_COVERAGE_RELATION"; premise="Epic coverage is computed from associated stories in Done status. Requirement coverage is a separate metric. No requirement-to-epic mapping is supplied. Whether covering a requirement improves epic coverage is unknown."; }
                else if(t.contains("bug ratio") || t.contains("bugratio")) key="bugRatio";
                else if(t.contains("defect score") || t.contains("defectscore") || t.contains("defect quality")) key="defectScore";
                else if(t.contains("confidence")) { ref="M_CONFIDENCE_GOVERNANCE"; premise="Official confidence="+official.path("confidence").asText("UNKNOWN")+". Confidence in analysis does not justify release approval or establish release safety. Human approval is required."; }
                else if(t.contains("epic coverage")) { ref="M_EPIC_COMPLETION"; premise="An epic is covered when it has at least one associated story and all associated stories are Done. Completing associated stories contributes to epic coverage."; }
                else if(t.contains("code smell")) key="codeSmells";
                else if(t.contains("sonar") || t.contains("vulnerabilit")) key="sonarCoverage";
                if(key!=null) {
                    JsonNode d=catalog.path("metrics").path(key);
                    if(d.isObject()) { ref="M_"+key; premise=d.path("interpretation").asText()+" Limitations: "+limitations(d.path("limitations")); if(official.hasNonNull(key)) premise+=" Official "+key+"="+official.get(key).asText()+"."; }
                }
            }
            if(ref!=null && out.size()<6 && !path.endsWith("/dimension") && !path.endsWith("/severity")) {
                ObjectNode p=out.putObject(e.getKey()); p.setAll((ObjectNode)e.getValue()); p.put("referenceId",ref); p.put("premise",premise);
            }
        });
        return out;
    }
    static String limitations(JsonNode node) {
        if(node.isTextual()) return node.asText();
        List<String> values=new ArrayList<>();
        if(node.isArray()) for(JsonNode item:node) if(item.isTextual()) values.add(item.asText());
        return String.join(" ",values);
    }
    public static String prompt(JsonNode pairs) {
        StringBuilder b=new StringBuilder(); pairs.fields().forEachRemaining(e->b.append("PAIR ").append(e.getKey()).append("\nPremise: ").append(e.getValue().path("premise").asText()).append("\nHypothesis: ").append(e.getValue().path("hypothesis").asText(e.getValue().path("evidence").asText())).append("\n\n")); return b.toString();
    }
    public static Map<String,Object> schema(JsonNode pairs) {
        List<String> ids=new ArrayList<>(); pairs.fieldNames().forEachRemaining(ids::add);
        return Map.of("type","object","additionalProperties",false,"required",List.of("assessments"),"properties",Map.of("assessments",Map.of("type","array","minItems",ids.size(),"maxItems",ids.size(),"items",Map.of("type","object","additionalProperties",false,"required",List.of("statementId","relation"),"properties",Map.of("statementId",Map.of("type","string","enum",ids.isEmpty()?List.of("NONE"):ids),"relation",Map.of("type","string","enum",RELATIONS))))));
    }
    public static ObjectNode validate(ObjectMapper m, JsonNode raw, JsonNode pairs) {
        if(!raw.isObject() || raw.size()!=1 || !raw.path("assessments").isArray() || raw.path("assessments").size()!=pairs.size()) throw new IllegalStateException("Incomplete NLI output");
        ObjectNode result=m.createObjectNode(); ArrayNode assessments=result.putArray("assessments"), findings=result.putArray("newFindings"); Set<String> seen=new HashSet<>();
        for(JsonNode a:raw.path("assessments")) {
            String id=a.path("statementId").asText(), relation=a.path("relation").asText();
            if(!a.isObject() || a.size()!=2 || !a.path("statementId").isTextual() || !a.path("relation").isTextual() || !pairs.has(id) || !seen.add(id) || !RELATIONS.contains(relation)) throw new IllegalStateException("Invalid NLI assessment");
            ObjectNode enriched=assessments.addObject(); enriched.setAll((ObjectNode)pairs.get(id)); enriched.put("statementId",id).put("relation",relation).put("semanticStatus","HUMAN_REVIEW_REQUIRED"); enriched.put("findingNature",relation.equals("SUPPORTED")?"NO_ALERT":relation.equals("CONTRADICTED")?"POTENTIAL_CONTRADICTION":"EVIDENCE_GAP");
            if(!relation.equals("SUPPORTED")) { ObjectNode f=findings.addObject(); f.setAll(enriched); f.put("category",relation.equals("CONTRADICTED")?"CONTRADICTION":"MISSING_CONTEXT").put("reason","Advisory NLI: "+relation+" against "+enriched.path("referenceId").asText()).put("evidenceStatus","QUOTE_VERIFIED"); }
        }
        result.put("verdict",findings.isEmpty()?"PASS":"NEEDS_IMPROVEMENT"); result.put("assessmentCoverage",pairs.isEmpty()?0:100); result.put("semanticQualityCertified",false); result.put("reasoning","Advisory classification of selected claims only; PASS is not certification."); return result;
    }
}
