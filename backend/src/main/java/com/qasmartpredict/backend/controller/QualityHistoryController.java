package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.QualitySnapshotDto;
import com.qasmartpredict.backend.service.QualitySnapshotService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class QualityHistoryController {

    private final QualitySnapshotService snapshotService;

    // =========================================================
    // Historique global - legacy
    // =========================================================

    @GetMapping("/history")
    public List<QualitySnapshotDto> history() {

        return snapshotService.getHistory();
    }

    // =========================================================
    // Historique d'une Release
    // =========================================================

    @GetMapping("/history/release/{releaseId}")
    public List<QualitySnapshotDto> historyByRelease(
            @PathVariable UUID releaseId) {

        return snapshotService.getHistory(releaseId);
    }

    // =========================================================
    // Snapshot global - legacy
    // =========================================================

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

    // =========================================================
    // Snapshot d'une Release
    // =========================================================

    @PostMapping("/snapshot/release/{releaseId}")
    public void saveSnapshotForRelease(
            @PathVariable UUID releaseId,
            @RequestParam double qis,
            @RequestParam double coverage,
            @RequestParam double defect,
            @RequestParam double feedback,
            @RequestParam double incident,
            @RequestParam double sonar) {

        snapshotService.saveSnapshot(
                releaseId,
                qis,
                coverage,
                defect,
                feedback,
                incident,
                sonar
        );
    }
}