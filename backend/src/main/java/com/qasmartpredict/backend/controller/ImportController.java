package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.service.imports.JiraImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ImportController {

    private final JiraImportService jiraImportService;

    @PostMapping(value = "/jira", consumes = "multipart/form-data")
    public String importJira(
            @RequestParam("file") MultipartFile file) {

        int count = jiraImportService.importCsv(file);

        return "Imported : " + count;
    }
}