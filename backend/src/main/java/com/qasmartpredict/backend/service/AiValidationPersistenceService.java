
package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AiValidationPersistenceService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AiValidationPersistenceService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {

        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID save(
            JsonNode sourceMetrics,
            String llamaResponse,
            String javaStatus,
            List<String> javaIssues,
            String javaError,
            String judgeStatus,
            JsonNode judgeResponse,
            String judgeError,
            String llamaModel,
            String judgeModel,
            long llamaDurationMs,
            Long judgeDurationMs) {

        UUID id = UUID.randomUUID();

        Integer judgeScore = null;
        String judgeIssues = "[]";

        if (judgeResponse != null) {

            JsonNode score = judgeResponse.path("score");

            if (score.isIntegralNumber()
                    && score.canConvertToInt()
                    && score.asInt() >= 0
                    && score.asInt() <= 100) {
                judgeScore = score.asInt();
            }

            if (judgeResponse.path("issues").isArray()) {
                judgeIssues = judgeResponse
                        .path("issues").toString();
            }
        }

        String sql = """
            INSERT INTO ai_validation_result (
                id,
                source_metrics,
                llama_response,
                java_status,
                java_issues,
                java_error,
                judge_status,
                judge_score,
                judge_issues,
                judge_response,
                judge_error,
                llama_model,
                judge_model,
                llama_duration_ms,
                judge_duration_ms
            )
            VALUES (
                ?,
                CAST(? AS jsonb),
                CAST(? AS jsonb),
                ?,
                CAST(? AS jsonb),
                ?,
                ?,
                ?,
                CAST(? AS jsonb),
                CAST(? AS jsonb),
                ?,
                ?,
                ?,
                ?,
                ?
            )
            """;

        jdbcTemplate.update(
                sql,
                id,
                sourceMetrics.toString(),
                llamaResponse,
                javaStatus,
                objectMapper.valueToTree(javaIssues).toString(),
                javaError,
                judgeStatus,
                judgeScore,
                judgeIssues,
                judgeResponse == null
                        ? null : judgeResponse.toString(),
                judgeError,
                llamaModel,
                judgeModel,
                llamaDurationMs,
                judgeDurationMs
        );

        System.out.println(
                "[AI PERSISTENCE] Saved validation: " + id
        );

        return id;
    }
	
	

	
}
