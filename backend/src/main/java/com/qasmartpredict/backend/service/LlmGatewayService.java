
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

        // 180 seconds for Ollama to generate the response
        requestFactory.setReadTimeout(180_000);

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

        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "format", "json",
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
                        "temperature", 0.2,
                        "num_predict", 1024
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
