package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.ReleaseEntity;
import com.qasmartpredict.backend.dto.CoverageSummaryDto;
import com.qasmartpredict.backend.dto.DefectSummaryDto;
import com.qasmartpredict.backend.dto.IncidentSummaryDto;
import com.qasmartpredict.backend.dto.ReleaseQualityDto;
import com.qasmartpredict.backend.repository.ReleaseRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReleaseQualityService {

    private final ReleaseRepository releaseRepository;
    private final CoverageService coverageService;
    private final DefectService defectService;
    private final IncidentService incidentService;


    @Transactional(readOnly = true)
    public ReleaseQualityDto calculate(UUID releaseId) {

        // 1. Vérifier que la release existe
        ReleaseEntity release =
                releaseRepository.findById(releaseId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Release not found: " + releaseId
                                )
                        );


        // 2. Coverage de la release
        CoverageSummaryDto coverage =
                coverageService
                        .calculateCoverageSummary(releaseId);


        // 3. Defect Quality de la release
        DefectSummaryDto defect =
                defectService
                        .calculateDefectScore(releaseId);


        // 4. Incident + Feedback de la release
        IncidentSummaryDto incident =
                incidentService
                        .calculate(releaseId);


        // 5. Construction du résultat
        return new ReleaseQualityDto(

                release.getId(),
                release.getVersion(),

                coverage.getCoveragePercentage(),

                defect.getDefectScore(),

                incident.feedbackScore(),

                incident.incidentScore(),

                coverage.getTotalRequirements(),
                coverage.getCoveredRequirements(),

                defect.getTotalStories(),
                defect.getTotalBugs(),

                incident.totalIncidents(),
                incident.criticalClientBugs()
        );
    }
}