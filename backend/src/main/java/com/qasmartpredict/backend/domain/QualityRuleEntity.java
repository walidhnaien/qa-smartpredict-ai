package com.qasmartpredict.backend.domain;

import jakarta.persistence.*;
import lombok.*;
 import java.math.BigDecimal;

import java.util.UUID;

@Entity
@Table(name = "quality_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityRuleEntity {

    @Id
    private UUID id;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(nullable = false)
    private Boolean enabled;
}