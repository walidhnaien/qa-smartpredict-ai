package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class OpenAiClientService {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    private final WebClient.Builder webClientBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generate(String prompt) {

        try {
            String body = """
            {
              "model": "%s",
              "input": %s
            }
            """.formatted(
                    model,
                    objectMapper.writeValueAsString(prompt)
            );

            String rawResponse = webClientBuilder.build()
                    .post()
                    .uri("https://api.openai.com/v1/responses")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(rawResponse);

            return root
                    .path("output")
                    .get(0)
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException("OpenAI generation failed", e);
        }
    }
}