package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.RequirementEntity;
import com.qasmartpredict.backend.domain.UserStoryEntity;
import com.qasmartpredict.backend.repository.RequirementRepository;
import com.qasmartpredict.backend.repository.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RequirementMatchingService {

    private final RequirementRepository requirementRepository;
    private final UserStoryRepository userStoryRepository;

    public int runMatching() {
        List<RequirementEntity> requirements = requirementRepository.findAll();
        List<UserStoryEntity> userStories = userStoryRepository.findAll();

        int linksCreated = 0;

        for (RequirementEntity requirement : requirements) {
            for (UserStoryEntity userStory : userStories) {

                if (matches(requirement, userStory)) {
                    boolean added = requirement.getUserStories().add(userStory);

                    if (added) {
                        linksCreated++;
                    }
                }
            }

            requirementRepository.save(requirement);
        }

        return linksCreated;
    }

    private boolean matches(RequirementEntity requirement, UserStoryEntity userStory) {
        String reqCode = requirement.getCode();
        String summary = safe(userStory.getSummary());

        if ("REQ-001".equals(reqCode)) {
            return containsAny(summary,
                    "cleared",
                    "clearing date",
                    "top day",
                    "trade extract",
                    "trade");
        }

        if ("REQ-002".equals(reqCode)) {
            return containsAny(summary,
                    "giveup",
                    "give-up",
                    "give up");
        }

        if ("REQ-003".equals(reqCode)) {
            return containsAny(summary,
                    "consolidation",
                    "detailed",
                    "result");
        }

        return false;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}