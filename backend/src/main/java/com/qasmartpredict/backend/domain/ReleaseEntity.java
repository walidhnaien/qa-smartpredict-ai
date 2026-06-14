package com.qasmartpredict.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "release")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ReleaseEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private ApplicationEntity application;

    private String name;

    private String version;

    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}