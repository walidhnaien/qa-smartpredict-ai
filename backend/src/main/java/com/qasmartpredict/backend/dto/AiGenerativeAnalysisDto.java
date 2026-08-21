package com.qasmartpredict.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerativeAnalysisDto {

    private String executiveSummary;

    private String releaseRationale;

    private List<AiRiskDto> topRisks;

    private List<AiPriorityActionDto> priorityActions;

    private String managerMessage;

    private String confidenceExplanation;
}