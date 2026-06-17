package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.QualityIntelligenceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiPromptBuilderService {

    private final QualityIntelligenceService qisService;

    public String buildPrompt() {

        QualityIntelligenceDto qis =
                qisService.calculate();

        return """
You are a Senior QA Manager.

Analyze the following metrics:

Quality Intelligence Score: %s
Coverage Score: %s
Defect Score: %s
Feedback Score: %s
Incident Score: %s

Return ONLY a valid JSON object with exactly these fields:

{
  "summary": "",
  "executiveSummary": "",
  "riskLevel": "",
  "sprintDecision": "",
  "strengths": [],
  "weaknesses": [],
  "recommendations": []
}

Rules:
- riskLevel must be one of: LOW, MEDIUM, HIGH
- sprintDecision must be one of: GO, GO WITH RISKS, NO GO
- summary must be one short sentence
- executiveSummary must contain 3 to 5 professional sentences
- strengths, weaknesses and recommendations must be arrays of strings
- do not return markdown
- do not return explanations
- return JSON only
""".formatted(
        qis.getQualityIntelligenceScore(),
        qis.getCoverageScore(),
        qis.getDefectScore(),
        qis.getFeedbackScore(),
        qis.getIncidentScore()
);
    }
}