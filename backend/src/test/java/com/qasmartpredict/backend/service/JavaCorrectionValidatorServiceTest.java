
package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaCorrectionValidatorServiceTest {

    private final ObjectMapper mapper =
            new ObjectMapper();

    private final JavaCorrectionValidatorService validator =
            new JavaCorrectionValidatorService();

    private ObjectNode source;
    private ObjectNode candidate;
    private ObjectNode review;

    @BeforeEach
    void setUp() {

        source = mapper.createObjectNode();
        source.put("qis", 68.89);
        source.put("risk", "HIGH");

        candidate = source.deepCopy();

        review = mapper.createObjectNode();
        review.put("status", "FAIL");
        review.put("score", 45);
        review.putArray("issues");
    }

    private void addIssue(
            String field,
            String correction) {

        ObjectNode issue =
                review.withArray("issues").addObject();

        issue.put("field", field);
        issue.set("expected", source.get(field));
        issue.set("actual", candidate.get(field));
        issue.put("suggested_correction", correction);
    }

    private void assertStatus(String expected) {

        var result = validator.validate(
                source, candidate, review
        );

        assertEquals(
                expected,
                result.status(),
                result.issues().toString()
        );
    }

    @Test
    void shouldAcceptCorrectQisCorrection() {

        candidate.put("qis", 95);

        addIssue("qis", "Change qis to 68.89");

        assertStatus("PASS");
    }

    @Test
    void shouldRejectIncorrectQisCorrection() {

        candidate.put("qis", 95);

        addIssue(
                "qis",
                "Change qis to 95 in CANDIDATE"
        );

        var result = validator.validate(
                source, candidate, review
        );

        assertEquals("FAIL", result.status());

        assertTrue(
            result.issues().stream().anyMatch(
                issue -> issue.contains(
                    "expected=68.89 | proposed=95"
                )
            )
        );
    }

    @Test
    void shouldAcceptCorrectRiskCorrection() {

        candidate.put("risk", "LOW");

        addIssue("risk", "Change risk to HIGH");

        assertStatus("PASS");
    }

    @Test
    void shouldRejectIncorrectRiskCorrection() {

        candidate.put("risk", "LOW");

        addIssue("risk", "Change risk to LOW");

        assertStatus("FAIL");
    }

    @Test
    void shouldAcceptQuotedCorrection() {

        candidate.put("risk", "LOW");

        addIssue(
            "risk",
            "Change risk to 'HIGH' in CANDIDATE"
        );

        assertStatus("PASS");
    }

    @Test
    void shouldHandleUnknownCorrectionFormat() {

        candidate.put("qis", 95);

        addIssue(
            "qis",
            "Please adjust the quality score"
        );

        assertStatus("NOT_VERIFIABLE");
    }

    @Test
    void shouldHandleMissingCorrection() {

        candidate.put("qis", 95);

        ObjectNode issue =
                review.withArray("issues").addObject();

        issue.put("field", "qis");

        assertStatus("NOT_VERIFIABLE");
    }

    @Test
    void shouldHandleCorrectCandidate() {

        review.put("status", "PASS");

        assertStatus("NOT_APPLICABLE");
    }

    @Test
    void shouldRejectWrongCorrectionField() {

        candidate.put("qis", 95);

        addIssue("qis", "Change risk to 68.89");

        assertStatus("FAIL");
    }

    @Test
    void shouldHandleMissingSource() {

        var result = validator.validate(
                null, candidate, review
        );

        assertEquals(
            "NOT_VERIFIABLE",
            result.status()
        );
    }

    @Test
    void shouldAcceptEquivalentNumericValues() {

        candidate.put("qis", 95);

        addIssue("qis", "Change qis to 68.890");

        assertStatus("PASS");
    }

    @Test
    void shouldRejectMixedCorrections() {

        candidate.put("qis", 95);
        candidate.put("risk", "LOW");

        addIssue("qis", "Change qis to 95");
        addIssue("risk", "Change risk to HIGH");

        assertStatus("FAIL");
    }
}
