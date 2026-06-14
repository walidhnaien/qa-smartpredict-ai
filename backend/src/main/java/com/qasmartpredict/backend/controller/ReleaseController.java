package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.domain.ReleaseEntity;
import com.qasmartpredict.backend.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReleaseController {

    private final ReleaseService releaseService;

    @GetMapping
    public List<ReleaseEntity> getAllReleases() {
        return releaseService.findAll();
    }
}