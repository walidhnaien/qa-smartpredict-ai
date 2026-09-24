package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.RequirementEntity;
import com.qasmartpredict.backend.dto.CoverageDto;
import com.qasmartpredict.backend.dto.CoverageSummaryDto;
import com.qasmartpredict.backend.repository.RequirementRepository;
import com.qasmartpredict.backend.repository.UserStoryRepository;
import com.qasmartpredict.backend.domain.UserStoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoverageService {

    private final RequirementRepository requirementRepository;
    private final UserStoryRepository userStoryRepository;


    // =========================================================
    // CALCUL GLOBAL HISTORIQUE
    // =========================================================

    public List<CoverageDto> calculateCoverage() {

        return requirementRepository.findAll()
                .stream()
                .map(requirement -> {

                    CoverageDto dto = new CoverageDto();

                    dto.setRequirementCode(requirement.getCode());
                    dto.setRequirementTitle(requirement.getTitle());
                    dto.setLinkedStories(
                            requirement.getUserStories().size()
                    );
                    dto.setCovered(
                            !requirement.getUserStories().isEmpty()
                    );

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
                        .filter(r ->
                                !r.getUserStories().isEmpty())
                        .count();

        CoverageSummaryDto dto =
                new CoverageSummaryDto();

        dto.setTotalRequirements(totalRequirements);
        dto.setCoveredRequirements(
                (int) coveredRequirements
        );

        dto.setCoveragePercentage(
                totalRequirements == 0
                        ? 0.0
                        : coveredRequirements
                            * 100.0
                            / totalRequirements
        );

        return dto;
    }


    // =========================================================
    // CALCUL PAR RELEASE
    //
    // REGLE MVP :
    // EPIC JIRA = REQUIREMENT
    // =========================================================

public CoverageSummaryDto calculateCoverageSummary(UUID releaseId) {

    List<UserStoryEntity> epics =
            userStoryRepository
                    .findDistinctByReleases_IdAndIssueType(
                            releaseId,
                            "Epic"
                    );

    int totalRequirements = epics.size();

    long coveredRequirements =
            epics.stream()
                    .filter(epic -> {

                        List<UserStoryEntity> stories =
                                userStoryRepository
                                        .findByEpicKey(
                                                epic.getJiraKey()
                                        );

                        // Epic sans Story = non couvert
                        if (stories.isEmpty()) {
                            return false;
                        }

                        // Toutes les Stories doivent être Done
                        return stories.stream()
                                .allMatch(story ->
                                        story.getStatus() != null
                                        && "Done".equalsIgnoreCase(
                                                story.getStatus().trim()
                                        )
                                );
                    })
                    .count();

    CoverageSummaryDto dto =
            new CoverageSummaryDto();

    dto.setTotalRequirements(totalRequirements);
    dto.setCoveredRequirements(
            (int) coveredRequirements
    );

    dto.setCoveragePercentage(
            totalRequirements == 0
                    ? 0.0
                    : coveredRequirements
                        * 100.0
                        / totalRequirements
    );

    return dto;
}
}