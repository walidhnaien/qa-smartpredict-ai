package com.qasmartpredict.backend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "import_job")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ImportJobEntity {

    @Id
    private UUID id;

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "file_name")
    private String fileName;

    private String status;

    @Column(name = "imported_records")
    private Integer importedRecords;

    @Column(name = "rejected_records")
    private Integer rejectedRecords;

    @Column(name = "import_date")
    private LocalDateTime importDate;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}