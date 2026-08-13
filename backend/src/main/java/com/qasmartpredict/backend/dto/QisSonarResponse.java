package com.qasmartpredict.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QisSonarResponse {

    private Long projectId;

    private Double baseQis;

    private Double sonarScore;

    private Double sonarWeight;

    private Double sonarContribution;

    private Double baseQisContribution;

    private Double finalQis;

    private String sonarQualityGate;

    private Integer bugs;

    private Integer vulnerabilities;

    private Integer codeSmells;

    private Double coverage;

    private Double duplicatedLinesDensity;
}