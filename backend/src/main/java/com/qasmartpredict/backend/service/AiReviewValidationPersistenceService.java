
package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AiReviewValidationPersistenceService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AiReviewValidationPersistenceService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {

        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID save(
            UUID reviewResultId,
            UUID batchId,
            String validatorVersion,
            String validationStatus,
            List<String> validationIssues,
            String correctionStatus,
            JsonNode deterministicCorrections,
            long durationMs,
            String errorMessage) {

        UUID id = UUID.randomUUID();

        String sql = """
            INSERT INTO ai_review_validation_history (
                id,
                review_result_id,
                batch_id,
                validator_version,
                validation_status,
                validation_issues,
                correction_status,
                deterministic_corrections,
                duration_ms,
                error_message
            )
            VALUES (
                ?, ?, ?, ?, ?,
                CAST(? AS jsonb),
                ?,
                CAST(? AS jsonb),
                ?, ?
            )
            """;

        jdbcTemplate.update(
                sql,
                id,
                reviewResultId,
                batchId,
                validatorVersion,
                validationStatus,
                objectMapper.valueToTree(
                        validationIssues
                ).toString(),
                correctionStatus,
                deterministicCorrections.toString(),
                durationMs,
                errorMessage
        );

        return id;
    }
}
