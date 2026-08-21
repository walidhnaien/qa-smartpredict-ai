package com.qasmartpredict.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiForecastDto {

    private double predictedNextQis;

    private String trend;

    private String expectedReleaseStatus;
}