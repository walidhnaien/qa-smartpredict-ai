
package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class AiReviewReplayService {

    private static final String VALIDATOR_VERSION = "1.1.0";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final JavaReviewValidatorService validator;
	private final JavaCorrectionValidatorService correctionValidator;

    private final AiReviewValidationPersistenceService
            persistenceService;

    public AiReviewReplayService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            JavaReviewValidatorService validator,
            AiReviewValidationPersistenceService persistenceService,
			JavaCorrectionValidatorService correctionValidator) {

        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.persistenceService = persistenceService;
		this.correctionValidator = correctionValidator;
    }

    public record ReplaySummary(
            UUID batchId,
            String validatorVersion,
            int total,
            int passed,
            int failed,
            int notVerifiable,
            int errors
    ) {}

    public ReplaySummary replayAll() {

        UUID batchId = UUID.randomUUID();

        String sql = """
            SELECT
                r.id,
                t.test_case_key,
                t.source_of_truth::text AS source_json,
                t.candidate::text AS candidate_json,
                r.parsed_response::text AS review_json
            FROM ai_review_result r
            JOIN ai_review_test_case t
                ON t.id = r.test_case_id
            ORDER BY t.test_case_key, r.created_at
            """;

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(sql);

        int passed = 0;
        int failed = 0;
        int notVerifiable = 0;
        int errors = 0;

        for (Map<String, Object> row : rows) {

            UUID reviewId = (UUID) row.get("id");

            String testCase =
                    (String) row.get("test_case_key");

            long start = System.nanoTime();

            String status;
            String correctionStatus = "NOT_RUN";
            String errorMessage = null;

            List<String> issues = new ArrayList<>();

            ArrayNode corrections =
                    objectMapper.createArrayNode();

            try {

                String reviewJson =
                        (String) row.get("review_json");

                if (reviewJson == null) {

                    status = "NOT_VERIFIABLE";
                    correctionStatus = "NOT_VERIFIABLE";

                    issues.add(
                        "Historical review response unavailable"
                    );

                } else {

                    JsonNode source = objectMapper.readTree(
                        (String) row.get("source_json")
                    );

                    JsonNode candidate = objectMapper.readTree(
                        (String) row.get("candidate_json")
                    );

                    JsonNode review =
                            objectMapper.readTree(reviewJson);

                    var result = validator.validate(
                            source,
                            candidate,
                            review
                    );

                    status = result.status();
                    issues.addAll(result.issues());

if (!"ERROR".equals(status)) {

    corrections = buildCorrections(
            source, candidate
    );

    var correctionResult =
            correctionValidator.validate(
                    source,
                    candidate,
                    review
            );

    correctionStatus = correctionResult.status();

    issues.addAll(correctionResult.issues());

    if ("FAIL".equals(correctionStatus)) {
        status = "FAIL";

    } else if ("PASS".equals(status)
            && "NOT_VERIFIABLE".equals(correctionStatus)) {

        status = "NOT_VERIFIABLE";
    }
}
                }

            } catch (Exception e) {

                status = "ERROR";
                errorMessage = e.getMessage();

                issues.add("Replay execution error");
            }

            long durationMs =
                    TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - start
                    );

            persistenceService.save(
                    reviewId,
                    batchId,
                    VALIDATOR_VERSION,
                    status,
                    issues,
                    correctionStatus,
                    corrections,
                    durationMs,
                    errorMessage
            );

            switch (status) {

                case "PASS" -> passed++;
                case "FAIL" -> failed++;
                case "NOT_VERIFIABLE" -> notVerifiable++;
                default -> errors++;
            }

            System.out.println(
                    "[AI REVIEW REPLAY] "
                    + testCase
                    + " | status=" + status
                    + " | issues=" + issues.size()
                    + " | duration=" + durationMs + " ms"
            );
        }

        return new ReplaySummary(
                batchId,
                VALIDATOR_VERSION,
                rows.size(),
                passed,
                failed,
                notVerifiable,
                errors
        );
    }

    private ArrayNode buildCorrections(
            JsonNode source,
            JsonNode candidate) {

        ArrayNode corrections =
                objectMapper.createArrayNode();

        Iterator<String> fields =
                candidate.fieldNames();

        while (fields.hasNext()) {

            String field = fields.next();

            if (!source.has(field)) {
                continue;
            }

            String correction =
                    validator.generateCorrection(
                            field, source, candidate
                    );

            if ("No correction needed".equals(correction)) {
                continue;
            }

            ObjectNode item = corrections.addObject();

            item.put("field", field);
            item.set("expected", source.get(field));
            item.set("actual", candidate.get(field));
            item.put("correction", correction);
        }

        return corrections;
    }
}
