package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiClientService {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    @Value("${ai.base-url}")
    private String baseUrl;

    private final WebClient.Builder webClientBuilder;

    private final ObjectMapper objectMapper;


    public String generate(String prompt) {

        WebClient webClient =
                webClientBuilder
                        .baseUrl(baseUrl)
                        .defaultHeader(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + apiKey
                        )
                        .defaultHeader(
                                HttpHeaders.CONTENT_TYPE,
                                MediaType.APPLICATION_JSON_VALUE
                        )
                        .build();


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


        Map<String, Object> requestBody =
                Map.of(
                        "model",
                        model,

                        "messages",
                        List.of(
                                Map.of(
                                        "role",
                                        "system",
                                        "content",
                                        systemPrompt
                                ),

                                Map.of(
                                        "role",
                                        "user",
                                        "content",
                                        prompt
                                )
                        ),

                        "temperature",
                        0.2
                );


        try {

            System.out.println(
                    "========== AI CLIENT CALL =========="
            );

            System.out.println(
                    "BASE URL : " + baseUrl
            );

            System.out.println(
                    "MODEL : " + model
            );

            System.out.println(
                    "===================================="
            );


            String rawResponse =
                    webClient
                            .post()
                            .uri("/chat/completions")
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();


            if (rawResponse == null
                    || rawResponse.isBlank()) {

                throw new IllegalStateException(
                        "AI provider returned an empty response"
                );
            }


            JsonNode response =
                    objectMapper.readTree(
                            rawResponse
                    );


            System.out.println(
                    "SELECTED MODEL : "
                            + response
                                    .path("model")
                                    .asText("unknown")
            );


            String content =
                    response
                            .path("choices")
                            .path(0)
                            .path("message")
                            .path("content")
                            .asText("");


            if (content.isBlank()) {

                throw new IllegalStateException(
                        "No AI content returned. Full response: "
                                + rawResponse
                );
            }


            String cleaned =
                    cleanJson(content);


            System.out.println(
                    "========== AI CLIENT RAW CONTENT =========="
            );

            System.out.println(content);

            System.out.println(
                    "==========================================="
            );


            return cleaned;


        } catch (WebClientResponseException e) {

            System.err.println(
                    "========== AI CLIENT HTTP ERROR =========="
            );

            System.err.println(
                    "HTTP STATUS : "
                            + e.getStatusCode()
            );

            System.err.println(
                    "BODY : "
                            + e.getResponseBodyAsString()
            );

            System.err.println(
                    "BASE URL : "
                            + baseUrl
            );

            System.err.println(
                    "MODEL : "
                            + model
            );

            System.err.println(
                    "=========================================="
            );

            throw e;


        } catch (Exception e) {

            System.err.println(
                    "========== AI CLIENT ERROR =========="
            );

            System.err.println(
                    "MESSAGE : "
                            + e.getMessage()
            );

            System.err.println(
                    "BASE URL : "
                            + baseUrl
            );

            System.err.println(
                    "MODEL : "
                            + model
            );

            System.err.println(
                    "====================================="
            );

            throw new IllegalStateException(
                    "Unable to call AI provider",
                    e
            );
        }
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