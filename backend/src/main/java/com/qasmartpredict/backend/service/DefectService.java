package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.dto.DefectSummaryDto;
import com.qasmartpredict.backend.repository.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefectService {

    private final UserStoryRepository userStoryRepository;

    public DefectSummaryDto calculateDefectScore() {

        long stories =
                userStoryRepository.countByIssueType("Story");

        long bugs =
                userStoryRepository.countByIssueType("Bug");

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
                Math.max(0, 100 - ratio)
        );

        return dto;
    }
}