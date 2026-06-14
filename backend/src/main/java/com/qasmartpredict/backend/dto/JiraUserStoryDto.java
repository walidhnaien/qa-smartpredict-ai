package com.qasmartpredict.backend.dto;

import lombok.Data;

@Data
public class JiraUserStoryDto {

    private String issueKey;

    private String summary;

    private String description;

    private String status;

    private String priority;

    private String sprint;
}