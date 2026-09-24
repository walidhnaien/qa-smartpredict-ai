package com.qasmartpredict.backend.controller;

import com.qasmartpredict.backend.dto.CreateReleaseRequest;
import com.qasmartpredict.backend.dto.ReleaseDto;
import com.qasmartpredict.backend.service.ReleaseService;
import com.qasmartpredict.backend.dto.ReleaseQualityDto;
import com.qasmartpredict.backend.service.ReleaseQualityService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReleaseController {

    private final ReleaseService releaseService;
    private final ReleaseQualityService releaseQualityService;
    @GetMapping
    public List<ReleaseDto> getAllReleases() {
        return releaseService.findAll();
    }

    @GetMapping("/{id}")
    public ReleaseDto getRelease(
            @PathVariable UUID id) {

        return releaseService.findById(id);
    }
	
	@GetMapping("/{id}/quality")
public ReleaseQualityDto getReleaseQuality(
        @PathVariable UUID id) {

    return releaseQualityService.calculate(id);
}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReleaseDto createRelease(
            @RequestBody CreateReleaseRequest request) {

        return releaseService.create(request);
    }
}