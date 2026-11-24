package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Map;

/** Shared governance + skill loader. It does not replace deterministic Java validation. */
@Service
public class QaAgentSkillsService {
    public static final String POLICY_VERSION = "QA_AGENT_POLICY_V2";
    private final QaMetricsCatalogService metrics;
    private final String globalPolicy;
    private final Map<String,String> skills;

    public QaAgentSkillsService(QaMetricsCatalogService metrics) throws IOException {
        this.metrics = metrics;
        globalPolicy = read("qa-agents/global-policy.md");
        Map<String,String> loaded = new LinkedHashMap<>();
        for (String name : List.of("metric-interpretation", "qa-analysis", "independent-review",
                "critique-resolution", "quality-validation")) {
            loaded.put(name, read("qa-agents/skills/" + name + "/SKILL.md"));
        }
        this.skills = Map.copyOf(loaded);
    }

    private static String read(String path) throws IOException {
        try (InputStream stream = new ClassPathResource(path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String policyVersion() { return POLICY_VERSION; }
    public String catalogVersion() { return metrics.version(); }
    public JsonNode metricsCatalog() { return metrics.catalog(); }

    /** role: LLAMA_INITIAL, GEMMA, QWEN, LLAMA_REVISION. */
    public String systemFor(String role, String existingTaskInstructions) {
        List<String> enabled = switch (role) {
            case "LLAMA_INITIAL" -> List.of("metric-interpretation", "qa-analysis");
            case "GEMMA", "QWEN" -> List.of("metric-interpretation", "independent-review");
            case "LLAMA_REVISION" -> List.of("metric-interpretation", "critique-resolution", "quality-validation");
            default -> throw new IllegalArgumentException("Unsupported agent role: " + role);
        };
        StringBuilder out = new StringBuilder(2500);
        out.append("AGENT GOVERNANCE VERSION: ").append(POLICY_VERSION).append('\n');
        out.append("METRIC CATALOG VERSION: ").append(metrics.version()).append("\n\n");
        out.append(globalPolicy).append("\nROLE: ").append(role).append("\n");
        for (String name : enabled) {
            out.append("\nENABLED SKILL: ").append(name).append('\n').append(skills.get(name));
        }
        out.append("\nTASK-SPECIFIC INSTRUCTIONS (follow global constraints):\n")
                .append(existingTaskInstructions);
        return out.toString();
    }

    /** Keep full catalog on disk but inject only definitions used by this release. */
    public JsonNode relevantCatalog(JsonNode official) {
        ObjectNode snapshot = (ObjectNode) metrics.catalog();
        ObjectNode definitions = (ObjectNode) snapshot.path("metrics");
        Set<String> used = new LinkedHashSet<>(List.of(
            "bugRatio", "defectScore", "confidence", "riskLevel", "releaseDecision"));
        if (official != null && !official.isMissingNode()) {
            gatherNames(compactOfficial(official), used);
        }
        if (used.contains("qis") || used.contains("qisFinal")) {
            used.add("qisFinal"); used.add("businessQis"); used.add("sonarScore");
        }
        // Preserve all catalog definitions in resource; only reduce LLM token use.
        Iterator<String> names = definitions.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!used.contains(name)) names.remove();
        }
        ObjectNode compact = snapshot.objectNode();
        compact.put("catalogVersion", metrics.version());
        ObjectNode selected = compact.putObject("metrics");
        definitions.fields().forEachRemaining(entry -> {
            ObjectNode d = selected.putObject(entry.getKey());
            for (String key : List.of("formula", "unit", "coverageRule", "interpretation")) {
                JsonNode value = entry.getValue().get(key);
                if (value != null && !value.isNull()) d.set(key, value);
            }
        });
        return compact;
    }

    public ObjectNode compactOfficial(JsonNode official) {
        if (official == null || !official.isObject()) throw new IllegalArgumentException("Missing official metrics");
        ObjectNode copy = official.deepCopy();
        copy.remove(List.of("sourcePrompt", "sourceSystemPrompt", "agentPolicyVersion", "metricsCatalogVersion"));
        return copy;
    }

    private static void gatherNames(JsonNode node, Set<String> names) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                names.add(entry.getKey());
                gatherNames(entry.getValue(), names);
            });
        } else if (node.isArray()) {
            for (JsonNode child : node) gatherNames(child, names);
        }
    }

    /** Feed this context to the model; Markdown files are never loaded by Ollama automatically. */
    public String catalogContext(JsonNode official) {
        return "\nQA_METRICS_CATALOG (definitions only; official Java values take precedence):\n"
                + relevantCatalog(official).toString() + "\n";
    }

    public String catalogContext() {
        return catalogContext(null);
    }
}
