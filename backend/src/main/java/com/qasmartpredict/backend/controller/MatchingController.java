package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.service.RequirementMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MatchingController {

    private final RequirementMatchingService matchingService;

    @PostMapping("/requirements")
    public String runRequirementMatching() {
        int linksCreated = matchingService.runMatching();

        return "Links created: " + linksCreated;
    }
}