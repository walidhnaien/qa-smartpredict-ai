package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.DefectSummaryDto;
import com.qasmartpredict.backend.service.DefectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/defects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DefectController {

    private final DefectService defectService;

    @GetMapping("/summary")
    public DefectSummaryDto getSummary() {

        return defectService.calculateDefectScore();
    }
}