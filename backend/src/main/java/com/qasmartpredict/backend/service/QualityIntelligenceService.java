package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.QualityRuleEntity;
import com.qasmartpredict.backend.dto.CoverageSummaryDto;
import com.qasmartpredict.backend.dto.DefectSummaryDto;
import com.qasmartpredict.backend.dto.IncidentSummaryDto;
import com.qasmartpredict.backend.dto.QualityIntelligenceDto;
import com.qasmartpredict.backend.dto.RcaSummaryDto;
import com.qasmartpredict.backend.repository.QualityRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QualityIntelligenceService {

    private final CoverageService coverageService;
    private final DefectService defectService;
    private final IncidentService incidentService;
    private final RcaService rcaService;
    private final QualityRuleRepository qualityRuleRepository;

    public QualityIntelligenceDto calculate() {

        CoverageSummaryDto coverage =
                coverageService.calculateCoverageSummary();

        DefectSummaryDto defect =
                defectService.calculateDefectScore();

        IncidentSummaryDto incident =
                incidentService.calculate();

        RcaSummaryDto rca =
                rcaService.calculate();

        double incidentScore =
                incident.incidentScore();

        double feedbackScore =
                incident.feedbackScore();

        double rcaScore =
                rca.rcaScore();

        List<QualityRuleEntity> rules =
                qualityRuleRepository.findByEnabledTrue();

        Map<String, Double> weights =
                rules.stream()
                        .collect(Collectors.toMap(
                                QualityRuleEntity::getRuleName,
                                r -> r.getWeight().doubleValue()
                        ));

        double coverageWeight =
                weights.getOrDefault("COVERAGE", 0.0);

        double defectWeight =
                weights.getOrDefault("DEFECT", 0.0);

        double feedbackWeight =
                weights.getOrDefault("FEEDBACK", 0.0);

        double incidentWeight =
                weights.getOrDefault("INCIDENT", 0.0);

        double rcaWeight =
                weights.getOrDefault("RCA", 0.0);

        double totalWeight =
                coverageWeight
                        + defectWeight
                        + feedbackWeight
                        + incidentWeight
                        + rcaWeight;

        double finalScore = 0.0;

        if (totalWeight > 0) {

            finalScore =
                    (
                            coverage.getCoveragePercentage() * coverageWeight
                                    + defect.getDefectScore() * defectWeight
                                    + feedbackScore * feedbackWeight
                                    + incidentScore * incidentWeight
                                    + rcaScore * rcaWeight
                    )
                            / totalWeight;
        }

        QualityIntelligenceDto dto =
                new QualityIntelligenceDto();

        dto.setCoverageScore(
                coverage.getCoveragePercentage());

        dto.setDefectScore(
                defect.getDefectScore());

        dto.setFeedbackScore(
                feedbackScore);

        dto.setIncidentScore(
                incidentScore);

        dto.setRcaScore(
                rcaScore);

        dto.setQualityIntelligenceScore(
                Math.round(finalScore * 100.0) / 100.0);

        dto.setTotalRequirements(
                coverage.getTotalRequirements());

        dto.setCoveredRequirements(
                coverage.getCoveredRequirements());

        dto.setUncoveredRequirements(
                coverage.getTotalRequirements()
                        - coverage.getCoveredRequirements());

        dto.setTotalStories(
                defect.getTotalStories());

        dto.setTotalBugs(
                defect.getTotalBugs());

        dto.setBugRatio(
                defect.getBugRatio());

        dto.setTotalIncidents(
                incident.totalIncidents());

        dto.setCriticalClientBugs(
                incident.criticalClientBugs());

        dto.setMostImpactedClient(
                incident.mostImpactedClient());

        dto.setRcaDone(
                rca.done());

        dto.setRcaInProgress(
                rca.inProgress());

        dto.setRcaToDo(
                rca.toDo());
                dto.setIncidentsWithRca(
        rca.incidentsWithRca());

dto.setRcaCoverage(
        rca.rcaCoverage());

dto.setCorrectiveActionCoverage(
        rca.correctiveActionCoverage());

dto.setPreventiveActionCoverage(
        rca.preventiveActionCoverage());

dto.setClosureRate(
        rca.closureRate());

        return dto;
    }
}