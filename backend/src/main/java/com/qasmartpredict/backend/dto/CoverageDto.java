package com.qasmartpredict.backend.dto;

import lombok.Data;

@Data
public class CoverageDto {

    private String requirementCode;

    private String requirementTitle;

    private int linkedStories;

    private boolean covered;
}