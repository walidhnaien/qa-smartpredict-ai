
package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.service.AiReviewReplayService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/review")
public class AiReviewReplayController {

    private final AiReviewReplayService replayService;

    public AiReviewReplayController(
            AiReviewReplayService replayService) {

        this.replayService = replayService;
    }

    @PostMapping("/replay")
    public AiReviewReplayService.ReplaySummary replay() {

        return replayService.replayAll();
    }
}
