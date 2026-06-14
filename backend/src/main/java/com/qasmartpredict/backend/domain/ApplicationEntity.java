package com.qasmartpredict.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "application")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String owner;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}