
package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OpenRouterJudgeService {

    private final RestClient client;
    private final ObjectMapper mapper;
    private final String model;

    public OpenRouterJudgeService(
            @Value("${qa.llm.openrouter.api-key:}") String apiKey,
            @Value("${qa.llm.openrouter.model:openrouter/free}") String model,
            ObjectMapper mapper) {

        this.mapper = mapper;
        this.model = model;

        this.client = RestClient.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public JudgeResult validateWithMetadata(String qaMetrics, String llamaResponse) {
        String systemPrompt = """
            You are an independent QA analysis evaluator.

            Evaluate the Llama-generated QA analysis
            against the supplied source metrics.

            Check:
            - Consistency with supplied QA metrics
            - Correct interpretation of quality scores
            - Consistency with deterministic release decision
            - Consistency with deterministic risk level
            - Unsupported claims and recommendations

            A higher quality score means better quality.
            Do not invent missing information.

            Return ONLY a JSON object with:
            {
              "status": "PASS or FAIL",
              "score": 0,
              "issues": [],
              "explanation": "string"
            }

            Score is a subjective evaluation from 0 to 100.
            """;

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.0,
                "messages", List.of(
                        Map.of("role", "system",
                               "content", systemPrompt),
                        Map.of("role", "user",
                               "content",
                               "SOURCE METRICS:\n" + qaMetrics
                               + "\n\nLLAMA RESPONSE:\n"
                               + llamaResponse)
                )
        );

        JsonNode response = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("Empty OpenRouter response");
        }

        String content = response.path("choices")
                .path(0).path("message")
                .path("content").asText("");

        if (content.isBlank()) {
            throw new IllegalStateException("Empty judge content");
        }

        JsonNode verdict;

try {
    verdict = mapper.readTree(content);
} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
    throw new IllegalStateException(
        "OpenRouter returned invalid JSON",
        e
    );
}

        String status = verdict.path("status").asText();
        if (!status.equals("PASS") && !status.equals("FAIL")) {
            throw new IllegalStateException("Invalid judge verdict");
        }

        String selectedModel = response
        .path("model")
        .asText(null);

System.out.println(
        "[OPENROUTER JUDGE] SELECTED MODEL: "
                + selectedModel
);

return new JudgeResult(verdict, selectedModel);
    }
	
	
	public record JudgeResult(
        JsonNode verdict,
        String selectedModel
) {}


public JsonNode validate(
        String qaMetrics,
        String llamaResponse) {

    return validateWithMetadata(
            qaMetrics,
            llamaResponse
    ).verdict();
}


	
	
	
}
