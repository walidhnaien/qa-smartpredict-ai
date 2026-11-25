package com.qasmartpredict.backend.service.orchestrator;
import java.util.List;
public final class QaWorkflowPlan {
    private QaWorkflowPlan() {}
    public record Step(String name, String model) {}
    public static final List<Step> STEPS = List.of(
        new Step("COLLECT_METRICS", null), new Step("INITIAL_ANALYSIS", "llama3.2:3b"),
        new Step("EXTRACT_CLAIMS", null), new Step("GOVERNANCE_REVIEW", "gemma3-qa:4b"),
        new Step("METRIC_REVIEW", "qwen2.5:3b"), new Step("CONSOLIDATE", null),
        new Step("CONTROLLED_REVISION", "llama3.2:3b"), new Step("FINAL_VALIDATION", null));
}
