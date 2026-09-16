package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.IncidentSummaryDto;
import com.qasmartpredict.backend.service.IncidentService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
@CrossOrigin(origins = "*")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(
            IncidentService incidentService) {

        this.incidentService = incidentService;
    }

    @GetMapping("/summary")
    public IncidentSummaryDto summary() {

        return incidentService.calculate();
    }
}