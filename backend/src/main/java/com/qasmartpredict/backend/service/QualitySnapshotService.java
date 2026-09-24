package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.QualitySnapshotEntity;
import com.qasmartpredict.backend.dto.QualitySnapshotDto;
import com.qasmartpredict.backend.repository.QualitySnapshotRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QualitySnapshotService {

    private final QualitySnapshotRepository repository;

    // =========================================================
    // LEGACY : snapshot global
    // =========================================================

    public void saveSnapshot(
            double qis,
            double coverage,
            double defect,
            double feedback,
            double incident,
            double sonar) {

        QualitySnapshotEntity snapshot =
                buildSnapshot(
                        qis,
                        coverage,
                        defect,
                        feedback,
                        incident,
                        sonar
                );

        repository.save(snapshot);
    }

    // =========================================================
    // RELEASE : snapshot rattaché à une Release
    // =========================================================

    public void saveSnapshot(
            UUID releaseId,
            double qis,
            double coverage,
            double defect,
            double feedback,
            double incident,
            double sonar) {

        QualitySnapshotEntity snapshot =
                buildSnapshot(
                        qis,
                        coverage,
                        defect,
                        feedback,
                        incident,
                        sonar
                );

        snapshot.setReleaseId(releaseId);

        repository.save(snapshot);
    }

    // =========================================================
    // LEGACY : historique global
    // =========================================================

    public List<QualitySnapshotDto> getHistory() {

        return toDtos(
                repository.findTop20ByOrderByAnalysisDateDesc()
        );
    }

    // =========================================================
    // RELEASE : historique filtré
    // =========================================================

    public List<QualitySnapshotDto> getHistory(UUID releaseId) {

        return toDtos(
                repository
                    .findTop20ByReleaseIdOrderByAnalysisDateDesc(
                        releaseId
                    )
        );
    }

    // =========================================================
    // Construction snapshot
    // =========================================================

    private QualitySnapshotEntity buildSnapshot(
            double qis,
            double coverage,
            double defect,
            double feedback,
            double incident,
            double sonar) {

        QualitySnapshotEntity snapshot =
                new QualitySnapshotEntity();

        snapshot.setAnalysisDate(
                LocalDateTime.now()
        );

        snapshot.setQis(qis);
        snapshot.setCoverageScore(coverage);
        snapshot.setDefectScore(defect);
        snapshot.setFeedbackScore(feedback);
        snapshot.setIncidentScore(incident);
        snapshot.setSonarScore(sonar);

        return snapshot;
    }

    // =========================================================
    // Entity -> DTO
    // =========================================================

    private List<QualitySnapshotDto> toDtos(
            List<QualitySnapshotEntity> snapshots) {

        return snapshots
                .stream()

                // Chart.js : ancien -> récent
                .sorted(
                        Comparator.comparing(
                                QualitySnapshotEntity::getAnalysisDate
                        )
                )

                .map(snapshot ->
                        new QualitySnapshotDto(
                                snapshot.getAnalysisDate(),
                                snapshot.getQis(),
                                snapshot.getCoverageScore(),
                                snapshot.getDefectScore(),
                                snapshot.getFeedbackScore(),
                                snapshot.getIncidentScore(),
                                snapshot.getSonarScore()
                        )
                )

                .toList();
    }
}