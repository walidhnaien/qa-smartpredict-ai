package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.RcaSummaryDto;
import com.qasmartpredict.backend.service.RcaImportService;
import com.qasmartpredict.backend.service.RcaService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rca")
@CrossOrigin(origins = "*")
public class RcaController {

    private final RcaService rcaService;
    private final RcaImportService importService;

    public RcaController(
            RcaService rcaService,
            RcaImportService importService) {

        this.rcaService = rcaService;
        this.importService = importService;
    }

    /**
     * RCA global historique.
     */
    @GetMapping("/summary")
    public RcaSummaryDto summary() {

        return rcaService.calculate();
    }

    /**
     * RCA d'une Release.
     */
    @GetMapping("/release/{releaseId}")
    public RcaSummaryDto summaryByRelease(
            @PathVariable UUID releaseId) {

        return rcaService.calculate(releaseId);
    }

    /**
     * Import du snapshot RCA.
     */
    @PostMapping(
            value = "/import",
            consumes = "multipart/form-data"
    )
    public Map<String, Object> importCsv(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        int imported =
                importService.importCsv(file);

        return Map.of(
                "status", "SUCCESS",
                "fileName", file.getOriginalFilename(),
                "imported", imported
        );
    }
}