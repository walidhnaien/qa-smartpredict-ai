package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.CoverageDto;
import com.qasmartpredict.backend.service.CoverageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.qasmartpredict.backend.dto.CoverageSummaryDto;

import java.util.List;

@RestController
@RequestMapping("/api/coverage")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CoverageController {

    private final CoverageService coverageService;

    @GetMapping
    public List<CoverageDto> getCoverage() {

        return coverageService.calculateCoverage();
    }
	
	
	@GetMapping("/summary")
    public CoverageSummaryDto getCoverageSummary() {

    return coverageService
            .calculateCoverageSummary();
}
	
}