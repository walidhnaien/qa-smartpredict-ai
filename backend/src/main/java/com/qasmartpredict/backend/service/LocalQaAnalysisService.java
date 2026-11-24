package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qasmartpredict.backend.dto.AiGenerativeAnalysisDto;
import com.qasmartpredict.backend.dto.AiIntelligenceRequest;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.TimeUnit;

import java.util.List;

@Service
public class LocalQaAnalysisService {

    private final ObjectMapper objectMapper;
    private final LlmGatewayService llmGatewayService;
    private final QaAgentSkillsService agentSkills;
    private final AiOutputValidatorService aiOutputValidatorService;
    private final AiValidationPersistenceService aiValidationPersistenceService;

    public LocalQaAnalysisService(ObjectMapper objectMapper,
            LlmGatewayService llmGatewayService,
            AiOutputValidatorService aiOutputValidatorService,
            AiValidationPersistenceService aiValidationPersistenceService,
            QaAgentSkillsService agentSkills) {
        this.objectMapper = objectMapper;
        this.llmGatewayService = llmGatewayService;
        this.aiOutputValidatorService = aiOutputValidatorService;
        this.aiValidationPersistenceService = aiValidationPersistenceService;
        this.agentSkills = agentSkills;
    }

    public AiGenerativeAnalysisDto generateAnalysis(
            AiIntelligenceRequest qa,
            String releaseDecision,
            String riskLevel,
            String mainRisk,
            int confidence) {

        String prompt = buildPrompt(
                qa,
                releaseDecision,
                riskLevel,
                mainRisk,
                confidence
        );

        String systemPrompt = """
                You are a senior QA Quality Intelligence analyst.

                Analyze only the supplied QA metrics.

                Important rules:
                - Never calculate or modify the QIS.
                - Never invent metrics.
                - Never override the deterministic release decision.
                - Explain risks using only supplied data.
                - Rank the most important quality risks.
                - Produce actionable QA recommendations.
                - Keep the analysis concise and professional.

                Return ONLY valid JSON.

                Required JSON structure:

                {
                  "executiveSummary": "string",
                  "releaseRationale": "string",
                  "topRisks": [
                    {
                      "dimension": "string",
                      "severity": "LOW | MEDIUM | HIGH | CRITICAL",
                      "reason": "string"
                    }
                  ],
                  "priorityActions": [
                    {
                      "priority": 1,
                      "action": "string",
                      "expectedImpact": "string"
                    }
                  ],
                  "managerMessage": "string",
                  "confidenceExplanation": "string"
                }

                Do not add markdown.
                Do not add ```json.
                Do not add text before or after the JSON object.
                """;

try {

    // 1. Generate original Llama response

    long llamaStart = System.nanoTime();

    String groundedSystemPrompt = agentSkills.systemFor("LLAMA_INITIAL", systemPrompt);
    String groundedUserPrompt = prompt + agentSkills.catalogContext(objectMapper.valueToTree(qa));
    String json = llmGatewayService.generate(
            groundedSystemPrompt,
            groundedUserPrompt
    );

    long llamaDurationMs = TimeUnit.NANOSECONDS
            .toMillis(System.nanoTime() - llamaStart);

    // 2. Initialize validation results

    AiGenerativeAnalysisDto analysis = null;
    Exception parsingError = null;

    String javaStatus = "ERROR";
    List<String> javaIssues = List.of();
    String javaError = null;

    String judgeStatus = "NOT_RUN";
    JsonNode judgeResponse = null;
    String judgeError = null;
    String judgeModel = "DISABLED";
    Long judgeDurationMs = null;

    // 3. Parse and validate with Java

    try {
        analysis = objectMapper.readValue(
                json,
                AiGenerativeAnalysisDto.class
        );
    } catch (Exception e) {
        parsingError = e;
        javaError = "JSON mapping error: " + e.getMessage();
    }

    if (parsingError == null) {

        try {
            var javaVerdict =
                    aiOutputValidatorService.validate(
                            qa,
                            analysis,
                            riskLevel,
                            releaseDecision
                    );

            javaStatus = javaVerdict.status();
            javaIssues = javaVerdict.issues();

        } catch (Exception e) {
            javaStatus = "ERROR";
            javaError = e.getMessage();
        }
    }

    System.out.println(
            "[JAVA VALIDATOR] "
            + javaStatus
            + " - issues=" + javaIssues.size()
    );

    for (String issue : javaIssues) {
        System.out.println(
                "[JAVA VALIDATOR] ISSUE: " + issue
        );
    }

    // 4. External judge disabled; local reviewers run separately.

    // 5. Save original metrics and decisions

    ObjectNode sourceMetrics =
            objectMapper.valueToTree(qa);

    sourceMetrics.put("releaseDecision", releaseDecision);
    sourceMetrics.put("riskLevel", riskLevel);
    sourceMetrics.put("mainRisk", mainRisk);
    sourceMetrics.put("confidence", confidence);

    // Preserve the exact evaluation context
    sourceMetrics.put("sourcePrompt", groundedUserPrompt);
    sourceMetrics.put("sourceSystemPrompt", groundedSystemPrompt);
    sourceMetrics.put("agentPolicyVersion", agentSkills.policyVersion());
    sourceMetrics.put("metricsCatalogVersion", agentSkills.catalogVersion());

    // 6. Persist both validation results

    try {

        aiValidationPersistenceService.save(
                sourceMetrics,
                json,
                javaStatus,
                javaIssues,
                javaError,
                judgeStatus,
                judgeResponse,
                judgeError,
                llmGatewayService.getModel(),
                judgeModel,
                llamaDurationMs,
                judgeDurationMs
        );

    } catch (Exception e) {

        System.err.println(
                "[AI PERSISTENCE] ERROR: "
                + e.getMessage()
        );
    }

    // Preserve the existing parsing failure behavior

    if (parsingError != null) {
        throw parsingError;
    }

    // 7. Return original analysis without correction

    return analysis;

} catch (Exception e) {

    throw new IllegalStateException(
            "Ollama QA analysis failed",
            e
    );
}

  
	  
	  
	  
    }

    private String buildPrompt(
            AiIntelligenceRequest qa,
            String releaseDecision,
            String riskLevel,
            String mainRisk,
            int confidence) {

        return """
                Analyze this QA release.

                ==================================
                DETERMINISTIC QUALITY DECISION
                ==================================

                Release Decision: %s
                Risk Level: %s
                Main Risk: %s
                Confidence: %d%%

                ==================================
                QUALITY INTELLIGENCE
                ==================================

                QIS: %.2f

                Epic Coverage Score: %.2f
                Defect Score (quality score, not bug ratio): %.2f
                Feedback: %.2f
                Incident Stability: %.2f

                ==================================
                SONAR QUALITY
                ==================================

                Sonar Quality: %.2f

                Bugs: %s
                Vulnerabilities: %s
                Code Smells: %s
                Sonar Coverage: %s
                Duplication: %s

                ==================================
                REQUIREMENT TRACEABILITY
                ==================================

                Requirements Total: %s
                Covered Requirements: %s
                Uncovered Requirements: %s

                ==================================
                DEFECT INFORMATION
                ==================================

                Stories: %s
                Total Bugs: %s
                Bug Ratio (bugs per 100 stories): %s

                ==================================
                EXPECTED ANALYSIS
                ==================================

                Explain why the deterministic release decision
                is appropriate using only the supplied metrics.

                Identify the most important quality risks.

                Rank the recommended QA actions by priority.

                Do not calculate a new QIS.

                Do not change the supplied release decision.

                Do not invent missing metrics.
                """
                .formatted(
                        releaseDecision,
                        riskLevel,
                        mainRisk,
                        confidence,

                        qa.getQis(),

                        qa.getCoverageScore(),
                        qa.getDefectScore(),
                        qa.getFeedbackScore(),
                        qa.getIncidentScore(),

                        qa.getSonarScore(),

                        qa.getBugs(),
                        qa.getVulnerabilities(),
                        qa.getCodeSmells(),
                        qa.getSonarCoverage(),
                        qa.getDuplication(),

                        qa.getTotalRequirements(),
                        qa.getCoveredRequirements(),
                        qa.getUncoveredRequirements(),

                        qa.getTotalStories(),
                        qa.getTotalBugs(),
                        qa.getBugRatio()
                );
    }

}
