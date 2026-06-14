package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.domain.ApplicationEntity;
import com.qasmartpredict.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public List<ApplicationEntity> getAllApplications() {
        return applicationService.findAll();
    }
}