package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.AiGenerativeAnalysisDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qasmartpredict.backend.dto.AiIntelligenceRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiQaService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model:gpt-5}")
    private String model;

    public OpenAiQaService(
            @Value("${openai.api.key}") String apiKey,
            ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;

        this.restClient =
                RestClient.builder()
                        .baseUrl("https://api.openai.com/v1")
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

    String prompt = buildPrompt(qa,
                releaseDecision,
                riskLevel,
                mainRisk,
                confidence);

    Map<String, Object> riskSchema =
            Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "dimension", Map.of(
                                    "type", "string"
                            ),
                            "severity", Map.of(
                                    "type", "string",
                                    "enum",
                                    List.of(
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
                            Map.of(
                                    "type", "string"
                            ),

                            "releaseRationale",
                            Map.of(
                                    "type", "string"
                            ),

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
                            Map.of(
                                    "type", "string"
                            ),

                            "confidenceExplanation",
                            Map.of(
                                    "type", "string"
                            )
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


    Map<String, Object> body =
            Map.of(
                    "model", model,

                    "input", List.of(

                            Map.of(
                                    "role", "system",
                                    "content",
                                    """
                                    You are a senior QA Quality Intelligence analyst.

                                    Analyze only the supplied QA metrics.

                                    Important rules:

                                    - Never calculate or modify the QIS.
                                    - Never invent metrics.
                                    - Never override the deterministic release decision.
                                    - Use the supplied data to explain risks.
                                    - Rank the most important quality risks.
                                    - Produce actionable recommendations.
                                    - Write for a QA Lead, Test Manager or Release Manager.
                                    - Keep recommendations concrete and concise.
                                    """
                            ),

                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    ),

                    "text",
                    Map.of(
                            "format",
                            Map.of(
                                    "type", "json_schema",

                                    "name",
                                    "qa_intelligence_analysis",

                                    "strict", true,

                                    "schema", schema
                            )
                    )
            );


    JsonNode response =
            restClient
                    .post()
                    .uri("/responses")
                    .contentType(
                            MediaType.APPLICATION_JSON
                    )
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);


    String json =
            extractText(response);


    try {

        return objectMapper.readValue(
                json,
                AiGenerativeAnalysisDto.class
        );

    } catch (Exception e) {

        System.err.println("========== OPENAI ERROR ==========");
        System.err.println("Message: " + e.getMessage());
        System.err.println("Type: " + e.getClass().getName());
        e.printStackTrace();
        System.err.println("==================================");

        throw new IllegalStateException(
                "Unable to parse OpenAI structured response",
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
                Analyze this QA release:

                QIS: %.2f

                Functional Coverage: %.2f
                Defect Quality: %.2f
                Feedback: %.2f
                Incident Stability: %.2f

                Sonar Quality: %.2f

                Bugs: %s
                Vulnerabilities: %s
                Code Smells: %s
                Sonar Coverage: %s
                Duplication: %s

                Requirements Total: %s
                Covered Requirements: %s
                Uncovered Requirements: %s

                Stories: %s
                Total Bugs: %s
                Bug Ratio: %s
                """
                .formatted(
                        qa.getQis(),
                    releaseDecision,
                    riskLevel,
                    mainRisk,
                    confidence,

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


    private String extractText(JsonNode response) {

        if (response == null) {
            return "";
        }

        JsonNode output =
                response.path("output");

        for (JsonNode item : output) {

            JsonNode content =
                    item.path("content");

            for (JsonNode part : content) {

                if (
                        "output_text".equals(
                                part.path("type").asText()
                        )
                ) {
                    return part.path("text").asText();
                }
            }
        }

        return "";
    }
}