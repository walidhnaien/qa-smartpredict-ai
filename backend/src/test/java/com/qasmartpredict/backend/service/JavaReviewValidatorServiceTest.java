
package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaReviewValidatorServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private final JavaReviewValidatorService validator =
            new JavaReviewValidatorService();

    private ObjectNode source;
    private ObjectNode candidate;
    private ObjectNode review;

    @BeforeEach
    void setUp() throws Exception {

        source = (ObjectNode) mapper.readTree("""
            {
              "qis": 68.89,
              "defectQualityRating": "POOR",
              "risk": "HIGH",
              "decision": "GO_WITH_RISK"
            }
            """);

        candidate = (ObjectNode) mapper.readTree("""
            {
              "qis": 95,
              "defectQualityRating": "EXCELLENT",
              "risk": "LOW",
              "decision": "GO"
            }
            """);

        review = (ObjectNode) mapper.readTree("""
            {
              "status": "FAIL",
              "score": 28,
              "issues": [
                {
                  "field": "qis",
                  "expected": 68.89,
                  "actual": 95
                },
                {
                  "field": "defectQualityRating",
                  "expected": "POOR",
                  "actual": "EXCELLENT"
                },
                {
                  "field": "risk",
                  "expected": "HIGH",
                  "actual": "LOW"
                },
                {
                  "field": "decision",
                  "expected": "GO_WITH_RISK",
                  "actual": "GO"
                }
              ],
              "explanation": "Four discrepancies detected."
            }
            """);
    }

    private void assertViolation(String rule) {

        var result = validator.validate(
                source, candidate, review
        );

        assertEquals("FAIL", result.status());

        assertTrue(
                result.issues().stream()
                        .anyMatch(issue -> issue.startsWith(rule)),
                "Expected violation: " + rule
                        + ", actual: " + result.issues()
        );
    }

    @Test
    void rv01_shouldRejectInvalidStatus() {

        review.put("status", "MISMATCH");

        assertViolation("RV-01");
    }

    @Test
    void rv02_shouldRejectInvalidScore() {

        review.put("score", 120);

        assertViolation("RV-02");
    }

    @Test
    void rv03_shouldRejectPassWithIssues() {

        review.put("status", "PASS");

        assertViolation("RV-03");
    }

    @Test
    void rv04_shouldRejectFailWithoutIssues() {

        review.putArray("issues");

        assertViolation("RV-04");
    }

    @Test
    void rv05_shouldRejectIncorrectExpectedValue() {

        ObjectNode issue =
                (ObjectNode) review.path("issues").get(0);

        issue.put("expected", 95);

        assertViolation("RV-05");
    }

    @Test
    void rv06_shouldRejectIncorrectActualValue() {

        ObjectNode issue =
                (ObjectNode) review.path("issues").get(0);

        issue.put("actual", 68.89);

        assertViolation("RV-06");
    }

    @Test
    void rv07_shouldDetectFalsePositive() {

        candidate = source.deepCopy();

        review.put("status", "PASS");

        review.putArray("issues")
                .addObject()
                .put("field", "risk")
                .put("expected", "HIGH")
                .put("actual", "HIGH");

        assertViolation("RV-07");
    }

    @Test
    void rv08_shouldGenerateCorrectQisCorrection() {

        String correction = validator.generateCorrection(
                "qis", source, candidate
        );

        assertEquals(
                "Change qis from 95 to 68.89",
                correction
        );
    }

    @Test
    void rv09_shouldRejectDuplicateIssues() {

        review.withArray("issues")
                .add(review.path("issues").get(0).deepCopy());

        assertViolation("RV-09");
    }

    @Test
    void rv10_shouldDetectMissingDiscrepancy() {

        review.withArray("issues").remove(3);

        assertViolation("RV-10");
    }

    @Test
    void rv11_shouldAcceptCorrectNegativeReview() {

        var result = validator.validate(
                source, candidate, review
        );

        assertEquals("PASS", result.status());

        assertTrue(result.issues().isEmpty());
    }

    @Test
    void rv12_shouldAcceptCorrectPositiveReview() {

        candidate = source.deepCopy();

        review.put("status", "PASS");
        review.put("score", 100);
        review.putArray("issues");

        var result = validator.validate(
                source, candidate, review
        );

        assertEquals("PASS", result.status());

        assertTrue(result.issues().isEmpty());
    }
}
