package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.RcaSummaryDto;
import com.qasmartpredict.backend.service.RcaImportService;
import com.qasmartpredict.backend.service.RcaService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

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

    @GetMapping("/summary")
    public RcaSummaryDto summary() {

        return rcaService.calculate();
    }

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