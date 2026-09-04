package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qasmartpredict.backend.dto.AiGenerativeAnalysisDto;
import com.qasmartpredict.backend.dto.AiIntelligenceRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiQaService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String baseUrl;

    public OpenAiQaService(
            @Value("${ai.api-key}") String apiKey,
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.model}") String model,
            ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
        this.model = model;
        this.baseUrl = baseUrl;

        this.restClient =
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .defaultHeader(
                                "Authorization",
                                "Bearer " + apiKey
                        )
                        .build();
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

Map<String, Object> riskSchema =
        Map.of(
                "type", "object",
                "properties", Map.of(
                        "dimension", Map.of(
                                "type", "string"
                        ),
                        "severity", Map.of(
                                "type", "string",
                                "enum", List.of(
                                        "LOW",
                                        "MEDIUM",
                                        "HIGH",
                                        "CRITICAL"
                                )
                        ),
                        "reason", Map.of(
                                "type", "string"
                        )
                ),
                "required", List.of(
                        "dimension",
                        "severity",
                        "reason"
                ),
                "additionalProperties", false
        );

Map<String, Object> actionSchema =
        Map.of(
                "type", "object",
                "properties", Map.of(
                        "priority", Map.of(
                                "type", "integer"
                        ),
                        "action", Map.of(
                                "type", "string"
                        ),
                        "expectedImpact", Map.of(
                                "type", "string"
                        )
                ),
                "required", List.of(
                        "priority",
                        "action",
                        "expectedImpact"
                ),
                "additionalProperties", false
        );

Map<String, Object> schema =
        Map.of(
                "type", "object",
                "properties", Map.of(
                        "executiveSummary",
                        Map.of("type", "string"),

                        "releaseRationale",
                        Map.of("type", "string"),

                        "topRisks",
                        Map.of(
                                "type", "array",
                                "items", riskSchema
                        ),

                        "priorityActions",
                        Map.of(
                                "type", "array",
                                "items", actionSchema
                        ),

                        "managerMessage",
                        Map.of("type", "string"),

                        "confidenceExplanation",
                        Map.of("type", "string")
                ),
                "required", List.of(
                        "executiveSummary",
                        "releaseRationale",
                        "topRisks",
                        "priorityActions",
                        "managerMessage",
                        "confidenceExplanation"
                ),
                "additionalProperties", false
        );

Map<String, Object> responseFormat =
        Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "qa_intelligence_analysis",
                        "strict", true,
                        "schema", schema
                )
        );

Map<String, Object> body =
        Map.of(
                "model", model,

                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),

                "response_format", responseFormat,

                "temperature", 0.2
        );

        try {

            System.out.println(
                    "========== AI PROVIDER CALL =========="
            );
            System.out.println("BASE URL : " + baseUrl);
            System.out.println("MODEL : " + model);
            System.out.println(
                    "======================================"
            );

            JsonNode response =
                    restClient
                            .post()
                            .uri("/chat/completions")
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(body)
                            .retrieve()
                            .body(JsonNode.class);

            System.out.println(
        "SELECTED MODEL : "
                + response.path("model").asText("unknown")
);

            String rawContent =
                    extractChatCompletionContent(response);

            String json =
                    cleanJson(rawContent);

            System.out.println(
                    "========== AI RAW RESPONSE =========="
            );
            System.out.println(rawContent);
            System.out.println(
                    "====================================="
            );

            return objectMapper.readValue(
                    json,
                    AiGenerativeAnalysisDto.class
            );

        } catch (HttpClientErrorException e) {

            System.err.println(
                    "========== AI PROVIDER HTTP ERROR =========="
            );
            System.err.println(
                    "HTTP STATUS : " + e.getStatusCode()
            );
            System.err.println(
                    "BODY : " + e.getResponseBodyAsString()
            );
            System.err.println(
                    "BASE URL : " + baseUrl
            );
            System.err.println(
                    "MODEL : " + model
            );
            System.err.println(
                    "============================================"
            );

            throw e;

        } catch (Exception e) {

            System.err.println(
                    "========== AI PARSING ERROR =========="
            );
            System.err.println(
                    "Message: " + e.getMessage()
            );
            System.err.println(
                    "Type: " + e.getClass().getName()
            );
            e.printStackTrace();
            System.err.println(
                    "======================================"
            );

            throw new IllegalStateException(
                    "Unable to parse OpenRouter AI response",
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

                Functional Coverage: %.2f
                Defect Quality: %.2f
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
                Bug Ratio: %s

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

    private String extractChatCompletionContent(
            JsonNode response) {

        if (response == null) {
            throw new IllegalStateException(
                    "Empty OpenRouter response"
            );
        }

        JsonNode content =
                response
                        .path("choices")
                        .path(0)
                        .path("message")
                        .path("content");

        if (content.isMissingNode()
                || content.isNull()
                || content.asText().isBlank()) {

            throw new IllegalStateException(
                    "No message content returned by OpenRouter: "
                            + response
            );
        }

        return content.asText();
    }

    private String cleanJson(String raw) {

        if (raw == null) {
            return "";
        }

        String cleaned =
                raw.trim();

        if (cleaned.startsWith("```json")) {
            cleaned =
                    cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned =
                    cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 3
                    );
        }

        int firstBrace =
                cleaned.indexOf('{');

        int lastBrace =
                cleaned.lastIndexOf('}');

        if (firstBrace >= 0
                && lastBrace > firstBrace) {

            cleaned =
                    cleaned.substring(
                            firstBrace,
                            lastBrace + 1
                    );
        }

        return cleaned.trim();
    }
}