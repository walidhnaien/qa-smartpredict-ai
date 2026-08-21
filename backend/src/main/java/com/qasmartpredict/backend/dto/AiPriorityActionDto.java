package com.qasmartpredict.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPriorityActionDto {

    private int priority;

    private String action;

    private String expectedImpact;
}