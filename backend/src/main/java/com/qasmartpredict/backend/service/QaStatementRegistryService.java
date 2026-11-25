package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

/** Neutral indexing: no semantic classification or anomaly hints. */
public final class QaStatementRegistryService {
    private QaStatementRegistryService() {}
    public static ObjectNode build(ObjectMapper mapper, JsonNode candidate) {
        ObjectNode out=mapper.createObjectNode();
        for(String field:List.of("executiveSummary","releaseRationale","managerMessage","confidenceExplanation"))
            add(out,candidate,"/"+field);
        for(String array:List.of("topRisks","priorityActions")) {
            JsonNode items=candidate.path(array);
            if(!items.isArray()) continue;
            for(int i=0;i<items.size();i++)
                for(String field:array.equals("topRisks")?List.of("dimension","severity","reason"):List.of("action","expectedImpact"))
                    add(out,candidate,"/"+array+"/"+i+"/"+field);
        }
        return out;
    }
    private static void add(ObjectNode out,JsonNode candidate,String path) {
        JsonNode value=candidate.at(path);
        if(value.isTextual()&&!value.asText().isBlank())
            out.putObject("S"+(out.size()+1)).put("sourcePath",path).put("evidence",value.asText());
    }
    public static ObjectNode prompt(ObjectMapper mapper,JsonNode registry) {
        ObjectNode out=mapper.createObjectNode();
        registry.fields().forEachRemaining(e->out.put(e.getKey(),e.getValue().path("sourcePath").asText()+": "+e.getValue().path("evidence").asText()));
        return out;
    }
    /** Preserve global IDs; overlapping fields allow cross-domain claims. */
    public static ObjectNode forRole(ObjectMapper mapper, JsonNode registry, String role) {
        if (!List.of("GEMMA","QWEN").contains(role)) throw new IllegalArgumentException("Unknown role");
        ObjectNode out=mapper.createObjectNode();
        registry.fields().forEachRemaining(e -> {
            String p=e.getValue().path("sourcePath").asText();
            // Keep narrative and actions for both roles: either may contain cross-domain errors.
            // Omit isolated labels from Qwen; keep them for governance severity checks.
            if (role.equals("GEMMA") || !(p.endsWith("/dimension") || p.endsWith("/severity")))
                out.set(e.getKey(),e.getValue());
        });
        return out;
    }
    public static ObjectNode textPrompt(ObjectMapper mapper, JsonNode registry) {
        ObjectNode out=mapper.createObjectNode();
        registry.fields().forEachRemaining(e -> out.put(e.getKey(),e.getValue().path("evidence").asText()));
        return out;
    }
    public static String error(JsonNode f,JsonNode registry) {
        if(!f.isObject()||f.size()!=3) return "INVALID_STRUCTURE";
        for(String key:List.of("statementId","category","reason"))
            if(!f.path(key).isTextual()||f.path(key).asText().isBlank()) return "INVALID_FIELD";
        if(!registry.has(f.path("statementId").asText())) return "UNKNOWN_STATEMENT";
        if(!AiDiscoveryValidationService.CATEGORIES.contains(f.path("category").asText())||f.path("reason").asText().length()>160) return "INVALID_LIMIT_OR_CATEGORY";
        return null;
    }
    public static ObjectNode resolve(JsonNode f,JsonNode registry) {
        String error=error(f,registry);
        if(error!=null) throw new IllegalArgumentException(error);
        ObjectNode out=(ObjectNode)f.deepCopy();
        JsonNode entry=registry.get(f.path("statementId").asText());
        out.set("sourcePath",entry.get("sourcePath")); out.set("evidence",entry.get("evidence"));
        return out;
    }
}
