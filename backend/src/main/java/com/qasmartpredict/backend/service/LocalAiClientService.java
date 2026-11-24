package com.qasmartpredict.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalAiClientService {
    private final LlmGatewayService llmGatewayService;

    public String generate(String prompt) {
        String systemPrompt = """
                You are a senior QA Quality Intelligence analyst.

                Analyze only the supplied QA metrics.

                Important rules:
                - Never invent QA metrics.
                - Never modify the deterministic QIS.
                - Never override the deterministic release decision.
                - Explain quality risks clearly.
                - Produce concrete QA recommendations.

                Return ONLY valid JSON.

                Required JSON structure:

                {
                  "riskLevel": "LOW | MEDIUM | HIGH | CRITICAL",
                  "summary": "string",
                  "executiveSummary": "string",
                  "recommendations": [
                    "string"
                  ],
                  "strengths": [
                    "string"
                  ],
                  "weaknesses": [
                    "string"
                  ],
                  "sprintDecision": "string"
                }

                Do not return markdown.
                Do not return ```json.
                Do not add text before or after the JSON.
                """;


        return llmGatewayService.generate(systemPrompt, prompt);
    }
}
