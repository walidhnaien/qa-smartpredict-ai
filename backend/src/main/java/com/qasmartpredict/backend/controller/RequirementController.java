package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.domain.RequirementEntity;
import com.qasmartpredict.backend.service.RequirementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requirements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RequirementController {

    private final RequirementService requirementService;

    @GetMapping
    public List<RequirementEntity> getAllRequirements() {
        return requirementService.findAll();
    }
}