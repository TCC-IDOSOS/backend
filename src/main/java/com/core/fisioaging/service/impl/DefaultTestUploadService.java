package com.core.fisioaging.service.impl;

import com.core.fisioaging.data.TestUploadData;
import com.core.fisioaging.data.TestUploadResponseData;
import com.core.fisioaging.enums.TestExecutionStatusEnum;
import com.core.fisioaging.model.TestExecutionModel;
import com.core.fisioaging.model.UserModel;
import com.core.fisioaging.repository.TestExecutionRepository;
import com.core.fisioaging.repository.UserRepository;
import com.core.fisioaging.service.TestUploadService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class DefaultTestUploadService implements TestUploadService {
    private final UserRepository userRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final String bucketName;

    public DefaultTestUploadService(
            UserRepository userRepository,
            TestExecutionRepository testExecutionRepository,
            S3Client s3Client,
            ObjectMapper objectMapper,
            @Value("${aws.s3.bucket}") String bucketName
    ) {
        this.userRepository = userRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.bucketName = bucketName;
    }

    @Override
    @Transactional
    public TestUploadResponseData uploadTest(String email, TestUploadData data) {
        validate(email, data);

        UserModel user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + email));

        String testType = data.getTipo_teste().trim().toUpperCase();

        TestExecutionModel execution = new TestExecutionModel();
        execution.setUser(user);
        execution.setTestType(testType);
        execution.setTestDateTime(data.getData_hora());
        execution.setSensor(data.getSensor());
        execution.setFrequency(data.getFrequencia());
        execution.setTotalRepetitionsApp(data.getTotal_repeticoes_app());
        execution.setStatus(TestExecutionStatusEnum.RECEIVED);
        execution.setCreatedAt(LocalDateTime.now());
        execution.setUpdatedAt(LocalDateTime.now());
        execution.setGenre(data.getSexo());
        execution.setAge(data.getIdade());
        execution.setWeight(data.getMassa_kg());

        execution = testExecutionRepository.save(execution);

        String s3Key = buildS3Key(user.getId(), testType, data.getData_hora(), execution.getId());
        String jsonPayload = serialize(data);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("application/json")
                    .metadata(Map.of(
                            "user-email", user.getEmail(),
                            "test-type", testType,
                            "test-execution-id", String.valueOf(execution.getId())
                    ))
                    .build();

            s3Client.putObject(request, RequestBody.fromString(jsonPayload));

            execution.setRawS3Key(s3Key);
            execution.setStatus(TestExecutionStatusEnum.UPLOADED);
            execution.setUpdatedAt(LocalDateTime.now());

            testExecutionRepository.save(execution);

            TestUploadResponseData response = new TestUploadResponseData();
            response.setTestExecutionId(execution.getId());
            response.setUserEmail(user.getEmail());
            response.setTestType(execution.getTestType());
            response.setStatus(execution.getStatus().name());
            response.setRawS3Key(execution.getRawS3Key());
            response.setMessage("Test uploaded successfully.");

            return response;
        } catch (Exception e) {
            execution.setStatus(TestExecutionStatusEnum.ERROR);
            execution.setUpdatedAt(LocalDateTime.now());
            testExecutionRepository.save(execution);
            throw new RuntimeException("Error uploading test file to S3.", e);
        }
    }

    private void validate(String email, TestUploadData data) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        if (data == null) {
            throw new IllegalArgumentException("Request body is required.");
        }

        if (data.getTipo_teste() == null || data.getTipo_teste().isBlank()) {
            throw new IllegalArgumentException("tipo_teste is required.");
        }

        String testType = data.getTipo_teste().trim().toUpperCase();
        if (!"UTT".equals(testType) && !"MARCHA".equals(testType)) {
            throw new IllegalArgumentException("tipo_teste must be UTT or MARCHA.");
        }

        if (data.getData_hora() == null || data.getData_hora().isBlank()) {
            throw new IllegalArgumentException("data_hora is required.");
        }

        if (data.getRegistros() == null || data.getRegistros().isEmpty()) {
            throw new IllegalArgumentException("registros must not be empty.");
        }
    }

    private String buildS3Key(Long userId, String testType, String dataHora, Long executionId) {
        return "raw/users/" + userId + "/" + testType.toLowerCase() + "/" + executionId + "_" + dataHora + ".json";
    }

    private String serialize(TestUploadData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializing test payload.", e);
        }
    }
}
