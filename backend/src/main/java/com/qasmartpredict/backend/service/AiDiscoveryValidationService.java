package com.qasmartpredict.backend.service;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** Verifies literal evidence only, never semantic truth. */
public final class AiDiscoveryValidationService {
    private AiDiscoveryValidationService() {}
    public static final List<String> CATEGORIES=List.of("UNSUPPORTED_CLAIM","CONTRADICTION","METRIC_CONFUSION","MISSING_CONTEXT","OTHER");
    public static String error(JsonNode f,JsonNode candidate) {
        if(!f.isObject() || f.size()!=4) return "INVALID_STRUCTURE";
        for(String key:List.of("category","sourcePath","evidence","reason"))
            if(!f.path(key).isTextual() || f.path(key).asText().isBlank()) return "INVALID_FIELD";
        String path=f.path("sourcePath").asText(), quote=f.path("evidence").asText();
        if(!CATEGORIES.contains(f.path("category").asText()) || path.length()>120 || quote.length()>240 || f.path("reason").asText().length()>160) return "INVALID_LIMIT_OR_CATEGORY";
        if(!path.matches("/(executiveSummary|releaseRationale|managerMessage|confidenceExplanation|topRisks/[0-9]+/(reason|severity|dimension)|priorityActions/[0-9]+/(action|expectedImpact))")) return "INVALID_PATH";
        JsonNode source=candidate.at(path);
        if(!source.isTextual() || !source.asText().contains(quote)) return "QUOTE_NOT_FOUND";
        return null;
    }
}
