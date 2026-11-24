package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.*;

/** Conservative checks of explicit assertions; not a general natural-language verifier. */
@Service
public class AiReviewSemanticValidationService {
    private static final List<String> ARRAYS = List.of("strengths", "weaknesses", "missingRisks", "suggestedImprovements", "unsupportedClaims", "questionsForQaManager");
    public JsonNode normalize(JsonNode input) {
        if (!input.isObject()) return input;
        ObjectNode copy = input.deepCopy();
        for (String field : ARRAYS) {
            if (!copy.path(field).isArray()) continue;
            ArrayNode clean = copy.arrayNode();
            Set<String> seen = new HashSet<>();
            for (JsonNode item : copy.path(field)) {
                if (!item.isTextual()) { clean.add(item); continue; }
                String value = item.asText().strip();
                if (!value.isEmpty() && seen.add(value.toLowerCase(Locale.ROOT))) clean.add(value);
            }
            copy.set(field, clean);
        }
        return copy;
    }
    public void validate(JsonNode official, JsonNode review) {
        if (official == null || !official.isObject()) throw new IllegalStateException("Missing official metrics");
        List<String> statements = new ArrayList<>();
        statements.add(review.path("reasoning").asText());
        for (String field : ARRAYS) for (JsonNode item : review.path(field)) statements.add(item.asText());
        for (String text : statements) {
            checkNumber(official, text, "confidence", "confidence(?:\\s+(?:level|score))?");
            checkNumber(official, text, "defectScore", "defect\\s*score");
            checkNumber(official, text, "bugRatio", "bug\\s*ratio");
            Matcher risk = Pattern.compile("(?i)\\b(low|medium|moderate|high|critical)\\s+risk\\s+level\\b|\\brisk\\s*level\\s*[:=]\\s*(low|medium|moderate|high|critical)\\b").matcher(text);
            while (risk.find()) {
                String value = risk.group(1) == null ? risk.group(2) : risk.group(1);
                if (official.path("riskLevel").isTextual() && !value.equalsIgnoreCase(official.path("riskLevel").asText()))
                    throw new IllegalStateException("Semantic review: risk level differs from official value");
            }
            if (Pattern.compile("(?i)\\bhigh\\s+defect\\s+quality\\s+ratio\\b").matcher(text).find())
                throw new IllegalStateException("Semantic review: ambiguous defect quality ratio");
        }
    }
    private void checkNumber(JsonNode official, String text, String key, String label) {
        JsonNode expected = official.path(key);
        if (!expected.isNumber()) return;
        Matcher m = Pattern.compile("(?i)\\b(?:" + label + ")\\s*(?:[:=]|is\\b|of\\b|\\()\\s*(\\d+(?:\\.\\d+)?)\\s*%?").matcher(text);
        while (m.find()) if (Math.abs(Double.parseDouble(m.group(1))-expected.asDouble()) > 0.011)
            throw new IllegalStateException("Semantic review: " + key + " differs from official value");
    }
}
