package com.qasmartpredict.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quality_snapshot")
@Getter
@Setter
public class QualitySnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime analysisDate;

    private double qis;

    private double coverageScore;

    private double defectScore;

    private double feedbackScore;

    private double incidentScore;

    private double sonarScore;
}