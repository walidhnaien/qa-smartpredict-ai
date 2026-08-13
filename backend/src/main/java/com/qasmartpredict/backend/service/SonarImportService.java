package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qasmartpredict.backend.dto.SonarMetric;
import com.qasmartpredict.backend.repository.SonarMetricRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SonarImportService {

    private final SonarMetricRepository sonarMetricRepository;
    private final SonarScoreService sonarScoreService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SonarMetric importStaticSonarResult(
            Long projectId,
            Long releaseId) throws IOException {

        // Lecture du fichier simulant le résultat Jenkins/Sonar
        ClassPathResource resource =
                new ClassPathResource("data/SonarResult.json");

        JsonNode root =
                objectMapper.readTree(resource.getInputStream());

        JsonNode component = root.get("component");

        if (component == null) {
            throw new IllegalArgumentException(
                    "Le fichier Sonar ne contient pas 'component'");
        }

        String projectKey =
                component.path("key").asText();

        JsonNode measures =
                component.path("measures");

        SonarMetric sonarMetric =
                SonarMetric.builder()
                        .projectId(projectId)
                        .releaseId(releaseId)
                        .projectKey(projectKey)
                        .qualityGate("UNKNOWN")
                        .build();

        for (JsonNode measure : measures) {

            String metric =
                    measure.path("metric").asText();

            String value =
                    measure.path("value").asText();

            switch (metric) {

                case "bugs" ->
                        sonarMetric.setBugs(
                                Integer.parseInt(value));

                case "vulnerabilities" ->
                        sonarMetric.setVulnerabilities(
                                Integer.parseInt(value));

                case "code_smells" ->
                        sonarMetric.setCodeSmells(
                                Integer.parseInt(value));

                case "coverage" ->
                        sonarMetric.setCoverage(
                                Double.parseDouble(value));

                case "duplicated_lines_density" ->
                        sonarMetric.setDuplicatedLinesDensity(
                                Double.parseDouble(value));

                case "sqale_index" ->
                        sonarMetric.setTechnicalDebt(
                                Double.parseDouble(value));

                default ->
                        System.out.println(
                                "Métrique Sonar ignorée : " + metric);
            }
        }

        // Calcul de notre score SmartPredict
        double sonarScore =
                sonarScoreService.calculateScore(sonarMetric);

        sonarMetric.setSonarScore(sonarScore);

        // Sauvegarde PostgreSQL
        return sonarMetricRepository.save(sonarMetric);
    }
}