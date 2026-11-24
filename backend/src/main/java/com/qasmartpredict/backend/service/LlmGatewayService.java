
package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class LlmGatewayService {

    private final RestClient ollamaClient;
    private final ObjectMapper objectMapper;
    private final String model;
	
			public String getModel() {
    return model;
}

    public LlmGatewayService(
            @Value("${qa.llm.ollama.base-url:http://ollama:11434}")
            String baseUrl,
            @Value("${qa.llm.ollama.model:llama3.2:3b}")
            String model,
            ObjectMapper objectMapper) {

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        // 10 seconds to establish the connection
        requestFactory.setConnectTimeout(10_000);

        // 240 seconds for Ollama to generate the response
        requestFactory.setReadTimeout(240_000);

        this.ollamaClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        this.model = model;
        this.objectMapper = objectMapper;
    }
	


    public String generate(
            String systemPrompt,
            String userPrompt) {

        return generateInternal(systemPrompt, userPrompt, "json", 1024, 0.2);
    }

    public String generateInitial(String systemPrompt, String userPrompt) {
        return generateInternal(systemPrompt, userPrompt, "json", 640, 0.1);
    }

    public String generateRevision(String systemPrompt, String userPrompt, Object schema) {
        return generateInternal(systemPrompt, userPrompt, schema, 512, 0.1);
    }

    private String generateInternal(String systemPrompt, String userPrompt, Object format, int budget, double temperature) {
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "keep_alive", 0,
                "format", format,
                "messages", new Object[] {
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )
                },
                "options", Map.of(
                        "temperature", temperature,
                        "num_predict", budget, "num_ctx", 4096, "repeat_penalty", 1.15
                )
        );

        long start = System.currentTimeMillis();

        try {
            System.out.println(
                    "[OLLAMA] Starting QA analysis - model: " + model
            );

            JsonNode response = ollamaClient
                    .post()
                    .uri("/api/chat")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new IllegalStateException(
                        "Empty Ollama response"
                );
            }

            System.out.println("[OLLAMA] prompt_tokens=" + response.path("prompt_eval_count")
                    + " completion_tokens=" + response.path("eval_count") + " done_reason=" + response.path("done_reason")
                    + " load_ns=" + response.path("load_duration") + " prompt_ns=" + response.path("prompt_eval_duration") + " eval_ns=" + response.path("eval_duration"));
            if ("length".equals(response.path("done_reason").asText())) throw new IllegalStateException("Truncated Ollama output");
            String content = response
                    .path("message")
                    .path("content")
                    .asText("");

            if (content.isBlank()) {
                throw new IllegalStateException(
                        "Ollama returned empty content"
                );
            }

            objectMapper.readTree(content);

            long duration =
                    System.currentTimeMillis() - start;

            System.out.println(
                    "[OLLAMA] QA analysis completed in "
                    + duration + " ms"
            );

            return content;

        } catch (Exception e) {

            long duration =
                    System.currentTimeMillis() - start;

            System.err.println(
                    "[OLLAMA] QA analysis failed after "
                    + duration + " ms: "
                    + e.getMessage()
            );

            throw new IllegalStateException(
                    "Ollama generation failed", e
            );
        }
    }
}
