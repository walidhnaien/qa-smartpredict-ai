package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.AiRecommendationDto;
import com.qasmartpredict.backend.dto.QualityIntelligenceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final QualityIntelligenceService qisService;

    public AiRecommendationDto generateRecommendations() {

        QualityIntelligenceDto qis =
                qisService.calculate();

        AiRecommendationDto dto =
                new AiRecommendationDto();

        double score =
                qis.getQualityIntelligenceScore();

        if(score < 60) {

            dto.setRiskLevel("HIGH");

            dto.setSummary(
                    "The project quality is critical and requires immediate action."
            );

        } else if(score < 80) {

            dto.setRiskLevel("MEDIUM");

            dto.setSummary(
                    "The project quality is acceptable but improvements are recommended."
            );
			String executiveSummary =
			"The project quality is acceptable with a QIS of "
			+ score +
			". Main weaknesses are requirement coverage and defect score.";

        } else {

            dto.setRiskLevel("LOW");

            dto.setSummary(
                    "The project quality is excellent."
            );
        }

        List<String> recommendations =
                new ArrayList<>();
				
		List<String> strengths =
        new ArrayList<>();

        List<String> weaknesses =
        new ArrayList<>();
		
		 if(qis.getCoverageScore() < 80) {

            recommendations.add(
                    "Improve requirement coverage."
            );
        }

        if(qis.getDefectScore() < 70) {

            recommendations.add(
                    "Reduce defect rate before next release."
            );
        }

        if(qis.getIncidentScore() < 90) {

            recommendations.add(
                    "Investigate production incidents."
            );
        }
		
		
		
		
		

        if(qis.getFeedbackScore() >= 80) {

    strengths.add(
            "Customer feedback score is high."
    );
	}
	

	if(qis.getIncidentScore() >= 80) {

		strengths.add(
				"Incident management is under control."
		);
	}

	if(qis.getCoverageScore() >= 80) {

		strengths.add(
				"Requirement coverage is satisfactory."
		);
	}


	if(qis.getCoverageScore() < 80) {

		weaknesses.add(
				"Coverage score below target."
		);
	}

	if(qis.getDefectScore() < 70) {

		weaknesses.add(
				"Defect score below target."
		);
	}

	
			if(score >= 80) {

			dto.setSprintDecision(
					"GO"
			);

		} else if(score >= 60) {

			dto.setSprintDecision(
					"GO WITH RISKS"
			);

		} else {

			dto.setSprintDecision(
					"NO GO"
			);
		}


		dto.setStrengths(
				strengths);

		dto.setWeaknesses(
				weaknesses);





        dto.setRecommendations(
                recommendations
        );

        return dto;
    }
}