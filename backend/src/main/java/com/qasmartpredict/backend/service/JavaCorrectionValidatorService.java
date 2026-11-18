
package com.qasmartpredict.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JavaCorrectionValidatorService {

    public record CorrectionResult(
            String status,
            List<String> issues
    ) {}

    private static final Pattern CORRECTION_PATTERN =
            Pattern.compile(
                "^Change\\s+(\\w+)\\s+to\\s+"
                + "['\"]?([\\w.\\-]+)['\"]?"
                + "(?:\\s+in\\s+CANDIDATE)?\\.?$",
                Pattern.CASE_INSENSITIVE
            );

    public CorrectionResult validate(
            JsonNode source,
            JsonNode candidate,
            JsonNode review) {

        if (source == null || !source.isObject()
                || candidate == null || !candidate.isObject()
                || review == null || !review.isObject()) {

            return new CorrectionResult(
                "NOT_VERIFIABLE",
                List.of("RV-08: Missing input data")
            );
        }

        List<String> errors = new ArrayList<>();
        List<String> unknown = new ArrayList<>();

        Set<String> expectedFields = new HashSet<>();
        Set<String> checkedFields = new HashSet<>();

        candidate.fieldNames().forEachRemaining(field -> {
            if (source.has(field)
                    && !sameValue(
                        source.get(field),
                        candidate.get(field))) {

                expectedFields.add(field);
            }
        });

        if (expectedFields.isEmpty()) {
            return new CorrectionResult(
                    "NOT_APPLICABLE", List.of()
            );
        }

        JsonNode issues = review.path("issues");

        if (!issues.isArray()) {
            return new CorrectionResult(
                "NOT_VERIFIABLE",
                List.of("RV-08: Invalid issues array")
            );
        }

        for (JsonNode issue : issues) {

            String field =
                    issue.path("field").asText("");

            if (!expectedFields.contains(field)) {
                unknown.add(
                    "RV-08: Unknown or unmatched field: "
                    + field
                );
                continue;
            }

            String correction = issue
                    .path("suggested_correction")
                    .asText("");

            Matcher matcher =
                    CORRECTION_PATTERN.matcher(
                            correction.trim()
                    );

            if (!matcher.matches()) {
                unknown.add(
                    "RV-08: Unrecognized correction format: "
                    + field
                );
                continue;
            }

            String correctionField = matcher.group(1);
            String target = matcher.group(2);

            checkedFields.add(field);

            if (!field.equals(correctionField)) {
                errors.add(
                    "RV-08: Incorrect correction field: "
                    + field
                );
                continue;
            }

            JsonNode expected = source.get(field);

            if (!matchesExpected(expected, target)) {

                errors.add(
                    "RV-08: Incorrect correction for "
                    + field
                    + " | expected=" + expected.asText()
                    + " | proposed=" + target
                );
            }
        }

        for (String field : expectedFields) {
            if (!checkedFields.contains(field)) {
                unknown.add(
                    "RV-08: Correction not verified: "
                    + field
                );
            }
        }

        if (!errors.isEmpty()) {
            errors.addAll(unknown);

            return new CorrectionResult(
                "FAIL", List.copyOf(errors)
            );
        }

        if (!unknown.isEmpty()) {
            return new CorrectionResult(
                "NOT_VERIFIABLE", List.copyOf(unknown)
            );
        }

        return new CorrectionResult(
            "PASS", List.of()
        );
    }

    private boolean matchesExpected(
            JsonNode expected, String target) {

        if (expected.isNumber()) {
            try {
                return expected.decimalValue()
                        .compareTo(new BigDecimal(target)) == 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return expected.isTextual()
                && expected.asText().equals(target);
    }

    private boolean sameValue(
            JsonNode first, JsonNode second) {

        if (first.isNumber() && second.isNumber()) {
            return first.decimalValue()
                    .compareTo(second.decimalValue()) == 0;
        }

        return first.equals(second);
    }
}
