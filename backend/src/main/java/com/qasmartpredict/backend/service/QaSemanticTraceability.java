package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;

/** Tracks advisory alerts without treating changed text as semantic resolution. */
public final class QaSemanticTraceability {
    private QaSemanticTraceability() {}
    public static ObjectNode build(ObjectMapper mapper, JsonNode assessments, JsonNode before, JsonNode after) {
        ObjectNode report=mapper.createObjectNode();
        var entries=report.putArray("alerts");
        Set<String> fields=new HashSet<>(), changed=new HashSet<>();
        int evaluated=0, contradictions=0, gaps=0;
        for(JsonNode wrapper:assessments) {
            JsonNode a=wrapper.path("assessment"); String relation=a.path("relation").asText();
            if(!Set.of("SUPPORTED","CONTRADICTED","NOT_ESTABLISHED").contains(relation)) continue;
            evaluated++;
            if("SUPPORTED".equals(relation)) continue;
            if("CONTRADICTED".equals(relation)) contradictions++; else gaps++;
            String path=a.path("sourcePath").asText();
            boolean valid=path.startsWith("/") && !before.at(path).isMissingNode();
            boolean modified=valid && !before.at(path).equals(after.at(path));
            if(valid) fields.add(path); if(modified) changed.add(path);
            ObjectNode entry=entries.addObject(); entry.set("assessment",a); entry.put("model",wrapper.path("model").asText());
            entry.put("sourceFieldChanged",modified); entry.put("status","HUMAN_REVIEW_REQUIRED");
            if(valid) {entry.set("before",before.at(path)); if(!after.at(path).isMissingNode()) entry.set("after",after.at(path)); else entry.putNull("after");}
        }
        report.put("evaluatedClaims",evaluated); report.put("potentialContradictions",contradictions); report.put("evidenceGaps",gaps);
        report.put("alertCount",entries.size()); report.put("uniqueAlertedFields",fields.size()); report.put("changedAlertedFields",changed.size());
        report.putNull("confirmedSemanticIssues"); report.putNull("confirmedResolvedSemanticIssues"); report.putNull("semanticIssueResolutionRate");
        report.put("status","HUMAN_REVIEW_REQUIRED"); return report;
    }
}
