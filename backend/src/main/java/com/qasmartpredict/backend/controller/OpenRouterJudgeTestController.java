
package com.qasmartpredict.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.qasmartpredict.backend.service.OpenRouterJudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/ai-judge")
@RequiredArgsConstructor
public class OpenRouterJudgeTestController {

    private final OpenRouterJudgeService judgeService;

    @PostMapping("/negative")
    public ResponseEntity<JsonNode> testNegative() {

        String metrics = """
            QIS: 68.89
            Risk Level: HIGH
            Release Decision: GO_WITH_RISK
            Coverage Quality Score: 80.00
            Defect Quality Score: 22.22
            Feedback Quality Score: 100.00
            Incident Quality Score: 57.14

            Higher quality scores mean better quality.
            """;

        String incorrectAnalysis = """
            {
              "executiveSummary":
                "The QIS is 95 and quality is excellent.",
              "releaseRationale":
                "The release decision is GO with LOW risk.",
              "topRisks": [],
              "priorityActions": [],
              "managerMessage":
                "Release immediately without restrictions.",
              "confidenceExplanation":
                "There are no quality risks."
            }
            """;

        JsonNode verdict = judgeService.validate(
                metrics,
                incorrectAnalysis
        );

        return ResponseEntity.ok(verdict);
    }
}
