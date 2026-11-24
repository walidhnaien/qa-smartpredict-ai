package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class QaMetricsCatalogService {
    private final JsonNode catalog;

    public QaMetricsCatalogService(ObjectMapper mapper) throws IOException {
        try (InputStream input = new ClassPathResource("qa-metrics-catalog.json").getInputStream()) {
            this.catalog = mapper.readTree(input);
        }
        if (catalog == null || !catalog.path("metrics").isObject()) {
            throw new IllegalStateException("Invalid QA metrics catalog");
        }
    }

    public JsonNode catalog() {
        return catalog.deepCopy();
    }

    public String version() {
        return catalog.path("catalogVersion").asText();
    }
}
