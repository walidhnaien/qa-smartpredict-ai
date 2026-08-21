package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.QualitySnapshotEntity;
import com.qasmartpredict.backend.dto.QualitySnapshotDto;
import com.qasmartpredict.backend.repository.QualitySnapshotRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QualitySnapshotService {

    private final QualitySnapshotRepository repository;


    public void saveSnapshot(
            double qis,
            double coverage,
            double defect,
            double feedback,
            double incident,
            double sonar) {

        QualitySnapshotEntity snapshot =
                new QualitySnapshotEntity();

        snapshot.setAnalysisDate(
                LocalDateTime.now());

        snapshot.setQis(qis);

        snapshot.setCoverageScore(coverage);

        snapshot.setDefectScore(defect);

        snapshot.setFeedbackScore(feedback);

        snapshot.setIncidentScore(incident);

        snapshot.setSonarScore(sonar);

        repository.save(snapshot);
    }


    public List<QualitySnapshotDto> getHistory() {

        return repository
                .findTop20ByOrderByAnalysisDateDesc()
                .stream()

                // Pour Chart.js :
                // ancien -> récent
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