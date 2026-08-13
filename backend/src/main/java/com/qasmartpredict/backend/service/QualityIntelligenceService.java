package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.QualityRuleEntity;
import com.qasmartpredict.backend.dto.CoverageSummaryDto;
import com.qasmartpredict.backend.dto.DefectSummaryDto;
import com.qasmartpredict.backend.dto.QualityIntelligenceDto;
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
    private final QualityRuleRepository qualityRuleRepository;

    public QualityIntelligenceDto calculate() {

        CoverageSummaryDto coverage =
                coverageService.calculateCoverageSummary();

        DefectSummaryDto defect =
                defectService.calculateDefectScore();

        double feedbackScore = 85.0;
        double incidentScore = 90.0;

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

       double totalWeight =
        coverageWeight
        + defectWeight
        + feedbackWeight
        + incidentWeight;

		double finalScore = 0.0;

		if (totalWeight > 0) {
			finalScore =
					(
							coverage.getCoveragePercentage() * coverageWeight
							+ defect.getDefectScore() * defectWeight
							+ feedbackScore * feedbackWeight
							+ incidentScore * incidentWeight
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

        dto.setQualityIntelligenceScore(
                Math.round(finalScore * 100.0) / 100.0);

        return dto;
    }
}