package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.QualitySnapshotDto;
import com.qasmartpredict.backend.service.QualitySnapshotService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class QualityHistoryController {

    private final QualitySnapshotService snapshotService;


    @GetMapping("/history")
    public List<QualitySnapshotDto> history() {

        return snapshotService.getHistory();
    }
	
	@PostMapping("/snapshot")
public void saveSnapshot(
        @RequestParam double qis,
        @RequestParam double coverage,
        @RequestParam double defect,
        @RequestParam double feedback,
        @RequestParam double incident,
        @RequestParam double sonar) {

    snapshotService.saveSnapshot(
            qis,
            coverage,
            defect,
            feedback,
            incident,
            sonar
    );
}
}