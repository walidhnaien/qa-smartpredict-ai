
package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class JavaReviewValidatorService {

    public record ValidationResult(
            String status,
            List<String> issues
    ) {}

    public ValidationResult validate(
            JsonNode source,
            JsonNode candidate,
            JsonNode review
    ) {

        List<String> errors = new ArrayList<>();

        if (review == null || !review.isObject()) {
            return new ValidationResult(
                    "FAIL",
                    List.of("RV-00: Invalid review JSON")
            );
        }

        if (source == null || !source.isObject()
                || candidate == null || !candidate.isObject()) {
            return new ValidationResult(
                    "ERROR",
                    List.of("Missing or invalid input data")
            );
        }

        String status = review.path("status").asText("");
        JsonNode score = review.path("score");
        JsonNode issues = review.path("issues");

        // RV-01: Validate reviewer status
        if (!Set.of("PASS", "FAIL").contains(status)) {
            errors.add("RV-01: Invalid status: " + status);
        }

        // RV-02: Validate score
        if (!score.isIntegralNumber()
                || !score.canConvertToInt()
                || score.asInt() < 0
                || score.asInt() > 100) {

            errors.add("RV-02: Invalid score");
        }

        if (!issues.isArray()) {
            errors.add("RV-00: Issues must be an array");

            return new ValidationResult(
                    "FAIL",
                    List.copyOf(errors)
            );
        }

        // RV-03: PASS must have no issues
        if ("PASS".equals(status) && !issues.isEmpty()) {
            errors.add("RV-03: PASS with non-empty issues");
        }

        // RV-04: FAIL must have issues
        if ("FAIL".equals(status) && issues.isEmpty()) {
            errors.add("RV-04: FAIL with empty issues");
        }

        Set<String> reportedFields = new HashSet<>();

        for (JsonNode issue : issues) {

            String field = issue.path("field").asText("");

            if (field.isBlank()) {
                errors.add("RV-00: Missing issue field");
                continue;
            }

            JsonNode expected = issue.get("expected");
            JsonNode actual = issue.get("actual");

            // RV-09: Duplicate issues
            if (!reportedFields.add(field)) {
                errors.add("RV-09: Duplicate issue: " + field);
            }

            // RV-05: Expected must match official data
            if (!sameValue(source.get(field), expected)) {
                errors.add(
                        "RV-05: Incorrect expected value: " + field
                );
            }

            // RV-06: Actual must match candidate data
            if (!sameValue(candidate.get(field), actual)) {
                errors.add(
                        "RV-06: Incorrect actual value: " + field
                );
            }

            // RV-07: An issue must represent a difference
            if (sameValue(expected, actual)) {
                errors.add(
                        "RV-07: False positive: " + field
                );
            }
        }

        // RV-10: Detect missing discrepancies
        Iterator<String> fields = candidate.fieldNames();

        while (fields.hasNext()) {

            String field = fields.next();

            if (!source.has(field)) {
                continue;
            }

            if (!sameValue(
                    source.get(field),
                    candidate.get(field))
                    && !reportedFields.contains(field)) {

                errors.add(
                        "RV-10: Missing discrepancy: " + field
                );
            }
        }

        return new ValidationResult(
                errors.isEmpty() ? "PASS" : "FAIL",
                List.copyOf(errors)
        );
    }

    private boolean sameValue(JsonNode first, JsonNode second) {

        if (first == null || second == null) {
            return false;
        }

        if (first.isNumber() && second.isNumber()) {
            return first.decimalValue()
                    .compareTo(second.decimalValue()) == 0;
        }

        return first.equals(second);
    }

    // RV-08: Generate a deterministic correction
    public String generateCorrection(
            String field,
            JsonNode source,
            JsonNode candidate
    ) {

        if (source == null || candidate == null
                || !source.has(field)
                || !candidate.has(field)) {

            throw new IllegalArgumentException(
                    "Unknown field: " + field
            );
        }

        if (sameValue(source.get(field), candidate.get(field))) {
            return "No correction needed";
        }

        return "Change " + field
                + " from " + candidate.get(field).asText()
                + " to " + source.get(field).asText();
    }
}
