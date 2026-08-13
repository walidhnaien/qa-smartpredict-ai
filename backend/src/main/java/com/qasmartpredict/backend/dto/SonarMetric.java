package com.qasmartpredict.backend.dto;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sonar_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SonarMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    private Long releaseId;

    @Column(nullable = false)
    private String projectKey;

    private String qualityGate;

    private Integer bugs;

    private Integer vulnerabilities;

    private Integer codeSmells;

    private Double coverage;

    private Double duplicatedLinesDensity;

    private Double technicalDebt;

    /*
     * Score calculé par QA SmartPredict.
     * 0 = très mauvaise qualité
     * 100 = excellente qualité
     */
    private Double sonarScore;

    private LocalDateTime analysisDate;

    @PrePersist
    public void prePersist() {
        if (analysisDate == null) {
            analysisDate = LocalDateTime.now();
        }
    }
}