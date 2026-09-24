package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.DefectSummaryDto;
import com.qasmartpredict.backend.repository.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefectService {

    private final UserStoryRepository userStoryRepository;


    /**
     * Ancien calcul global.
     *
     * Conservé temporairement pour ne pas casser
     * le dashboard actuel.
     */
// Calcul GLOBAL - on conserve pour compatibilité
public DefectSummaryDto calculateDefectScore() {

    long stories =
            userStoryRepository.countByIssueType("Story");

    long bugs =
            userStoryRepository.countByIssueType("Bug");

    return buildSummary(stories, bugs);
}


// Calcul PAR RELEASE
public DefectSummaryDto calculateDefectScore(UUID releaseId) {

    long stories =
            userStoryRepository
                    .countDistinctByReleases_IdAndIssueType(
                            releaseId,
                            "Story"
                    );

    long bugs =
            userStoryRepository
                    .countDistinctByReleases_IdAndIssueType(
                            releaseId,
                            "Bug"
                    );

    return buildSummary(stories, bugs);
}


    /**
     * Règle métier commune.
     */
    private DefectSummaryDto buildSummary(
            long stories,
            long bugs) {

        DefectSummaryDto dto =
                new DefectSummaryDto();

        dto.setTotalStories(stories);
        dto.setTotalBugs(bugs);

        if (stories == 0) {

            dto.setBugRatio(0);
            dto.setDefectScore(100);

            return dto;
        }

        double ratio =
                (bugs * 100.0) / stories;

        dto.setBugRatio(ratio);

        dto.setDefectScore(
                Math.max(
                        0,
                        100 - ratio
                )
        );

        return dto;
    }
}