package com.core.fisioaging.service.impl;

import com.core.fisioaging.data.TestResultAwsResponse;
import com.core.fisioaging.model.TestExecutionModel;
import com.core.fisioaging.model.TestResultModel;
import com.core.fisioaging.repository.TestExecutionRepository;
import com.core.fisioaging.repository.TestResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestResultSyncService {

    private final TestExecutionRepository testExecutionRepository;
    private final TestResultRepository testResultRepository;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Transactional
    public void syncNewResultsFromS3() {
        List<TestExecutionModel> executions = testExecutionRepository.findExecutionsWithoutResult();

        log.info("Found {} test executions without result.", executions.size());

        for (TestExecutionModel execution : executions) {
            processExecution(execution);
        }
    }

    private void processExecution(final TestExecutionModel execution) {
        if (testResultRepository.existsByTestExecutionId(execution.getId())) {
            return;
        }

        String rawKey = execution.getRawS3Key();

        if (rawKey == null || rawKey.isBlank()) {
            log.info("Test execution {} does not have rawS3Key yet.", execution.getId());
            return;
        }

        String processedKey = rawKey.replaceFirst("^raw/", "processed/");

        try {
            TestResultAwsResponse awsResult = readResultFromS3(processedKey);

            TestResultModel result = buildTestResult(execution, awsResult);

            testResultRepository.save(result);

            log.info(
                    "Saved test result for execution {} from S3 key {}.",
                    execution.getId(),
                    processedKey
            );

        } catch (NoSuchKeyException e) {
            log.info(
                    "Processed result not found yet for execution {}. Key: {}",
                    execution.getId(),
                    processedKey
            );
        } catch (Exception e) {
            log.error(
                    "Error processing result for execution {}. Key: {}",
                    execution.getId(),
                    processedKey,
                    e
            );
        }
    }

    private TestResultAwsResponse readResultFromS3(final String processedKey) throws IOException {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(processedKey)
                .build();

        try (ResponseInputStream<GetObjectResponse> obj = s3Client.getObject(request)) {
            byte[] body = obj.readAllBytes();
            return objectMapper.readValue(body, TestResultAwsResponse.class);
        }
    }

    private TestResultModel buildTestResult(
            final TestExecutionModel execution,
            final TestResultAwsResponse awsResult
    ) {
        TestResultModel result = new TestResultModel();

        result.setTestExecution(execution);

        result.setPatientName(execution.getUser().getName());
        result.setPatientAge(execution.getAge());
        result.setPatientSex(execution.getGenre());

        result.setTestType(execution.getTestType());
        result.setTestDateTime(parseTestDateTime(execution.getTestDateTime()));

        result.setHealthUnit(execution.getHealthUnit());

        result.setTotalRepetitions(awsResult.getTotalRepeticoes());
        result.setAverageHeight(awsResult.getAlturaMedia());
        result.setCadence(awsResult.getCadencia());
        result.setTestClassification(awsResult.getClassificacao());

        result.setCreatedAt(LocalDateTime.now());

        return result;
    }

    public static LocalDateTime parseTestDateTime(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        return LocalDateTime.parse(value, formatter);
    }
}
