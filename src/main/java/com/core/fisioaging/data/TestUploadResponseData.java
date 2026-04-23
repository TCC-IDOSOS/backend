package com.core.fisioaging.data;

import lombok.Data;

@Data
public class TestUploadResponseData {

    private Long testExecutionId;
    private String userEmail;
    private String testType;
    private String status;
    private String rawS3Key;
    private String message;
}
