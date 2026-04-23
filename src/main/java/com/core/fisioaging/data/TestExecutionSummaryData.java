package com.core.fisioaging.data;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestExecutionSummaryData {

    private Long id;
    private String testType;
    private String testDateTime;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer totalRepetitionsApp;
    private String rawS3Key;
    private String processedS3Key;
}
