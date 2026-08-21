package com.qasmartpredict.backend.service;

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

    private final WebClient.Builder webClientBuilder;


    public String generate(String prompt) {

        WebClient webClient =
                webClientBuilder
                        .baseUrl("https://api.openai.com/v1")
                        .defaultHeader(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + apiKey
                        )
                        .defaultHeader(
                                HttpHeaders.CONTENT_TYPE,
                                MediaType.APPLICATION_JSON_VALUE
                        )
                        .build();


        Map<String, Object> requestBody =
                Map.of(
                        "model", model,

                        "input", List.of(

                                Map.of(
                                        "role", "system",
                                        "content",
                                        "You are a senior QA Quality Intelligence analyst."
                                ),

                                Map.of(
                                        "role", "user",
                                        "content", prompt
                                )
                        )
                );


        try {

            String response =
                    webClient
                            .post()
                            .uri("/responses")
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();

            return response;

        } catch (WebClientResponseException e) {

            System.err.println(
                    "========== OPENAI API ERROR =========="
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
                    "======================================"
            );

            throw e;
        }
    }
}