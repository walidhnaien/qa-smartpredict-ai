package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.RequirementEntity;
import com.qasmartpredict.backend.dto.CoverageDto;
import com.qasmartpredict.backend.repository.RequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.qasmartpredict.backend.dto.CoverageSummaryDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoverageService {

    private final RequirementRepository requirementRepository;

    public List<CoverageDto> calculateCoverage() {

        return requirementRepository.findAll()
                .stream()
                .map(requirement -> {

                    CoverageDto dto = new CoverageDto();

                    dto.setRequirementCode(
                            requirement.getCode());

                    dto.setRequirementTitle(
                            requirement.getTitle());

                    dto.setLinkedStories(
                            requirement.getUserStories().size());

                    dto.setCovered(
                            requirement.getUserStories().size() > 0);

                    return dto;
                })
                .toList();
    }
	
	public CoverageSummaryDto calculateCoverageSummary() {

    List<RequirementEntity> requirements =
            requirementRepository.findAll();

    int totalRequirements = requirements.size();

    long coveredRequirements =
            requirements.stream()
                    .filter(r -> !r.getUserStories().isEmpty())
                    .count();

    CoverageSummaryDto dto =
            new CoverageSummaryDto();

    dto.setTotalRequirements(totalRequirements);

    dto.setCoveredRequirements(
            (int) coveredRequirements);

    if (totalRequirements == 0) {
        dto.setCoveragePercentage(0);
    } else {

        dto.setCoveragePercentage(
                coveredRequirements * 100.0
                        / totalRequirements);
    }

    return dto;
}
}