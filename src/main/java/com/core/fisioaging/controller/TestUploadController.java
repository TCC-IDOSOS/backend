package com.core.fisioaging.controller;

import com.core.fisioaging.data.TestExecutionSummaryData;
import com.core.fisioaging.data.TestUploadData;
import com.core.fisioaging.data.TestUploadResponseData;
import com.core.fisioaging.model.TestExecutionModel;
import com.core.fisioaging.repository.TestExecutionRepository;
import com.core.fisioaging.service.TestUploadService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "bearer")
public class TestUploadController {

    private static final Logger log = LoggerFactory.getLogger(TestUploadController.class);

    private final TestUploadService testUploadService;
    private final TestExecutionRepository testExecutionRepository;
    private final S3Client s3Client;
    private final String bucketName;

    public TestUploadController(
            TestUploadService testUploadService,
            TestExecutionRepository testExecutionRepository,
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucketName) {
        this.testUploadService = testUploadService;
        this.testExecutionRepository = testExecutionRepository;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @PostMapping("/tests")
    public ResponseEntity<?> uploadTest(
            @RequestParam("email") String email,
            @RequestBody TestUploadData data) {
        try {
            TestUploadResponseData response = testUploadService.uploadTest(email, data);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            TestUploadResponseData response = new TestUploadResponseData();
            response.setUserEmail(email);
            response.setMessage(e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            TestUploadResponseData response = new TestUploadResponseData();
            response.setUserEmail(email);
            response.setMessage("Internal server error.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // TODO (next iteration): suportar paginação e filtros (tipo de teste, intervalo de datas) — HU-014.
    @GetMapping("/tests")
    public ResponseEntity<List<TestExecutionSummaryData>> listTests(@RequestParam("email") String email) {
        List<TestExecutionSummaryData> tests = testExecutionRepository
                .findByUserEmailAndIsDeletedFalseOrderByCreatedAtDesc(email)
                .stream()
                .map(TestUploadController::toSummary)
                .toList();
        return ResponseEntity.ok(tests);
    }

    // Soft delete: marca o teste como excluído (is_deleted = true) em vez de removê-lo.
    @DeleteMapping("/tests/{id}")
    public ResponseEntity<Void> deleteTest(
            @PathVariable("id") Long id,
            @RequestParam("email") String email) {
        Optional<TestExecutionModel> maybe = testExecutionRepository.findById(id);
        if (maybe.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TestExecutionModel execution = maybe.get();
        if (!execution.getUser().getEmail().equals(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (Boolean.FALSE.equals(execution.getIsDeleted())) {
            execution.setIsDeleted(true);
            execution.setUpdatedAt(LocalDateTime.now());
            testExecutionRepository.save(execution);
        }
        return ResponseEntity.noContent().build();
    }

    private static TestExecutionSummaryData toSummary(TestExecutionModel m) {
        TestExecutionSummaryData d = new TestExecutionSummaryData();
        d.setId(m.getId());
        d.setTestType(m.getTestType());
        d.setTestDateTime(m.getTestDateTime());
        d.setStatus(m.getStatus() != null ? m.getStatus().name() : null);
        d.setCreatedAt(m.getCreatedAt());
        d.setUpdatedAt(m.getUpdatedAt());
        d.setTotalRepetitionsApp(m.getTotalRepetitionsApp());
        d.setRawS3Key(m.getRawS3Key());
        d.setProcessedS3Key(m.getProcessedS3Key());
        return d;
    }

    // TODO (next iteration): parse the Lambda output and persist to Resultado_Teste.
    // Currently streaming the S3 JSON directly; DB population happens later.
    @GetMapping("/tests/{id}/result")
    public ResponseEntity<?> getTestResult(
            @PathVariable("id") Long id,
            @RequestParam("email") String email) {
        Optional<TestExecutionModel> maybe = testExecutionRepository.findById(id);
        if (maybe.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        TestExecutionModel execution = maybe.get();
        if (!execution.getUser().getEmail().equals(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String rawKey = execution.getRawS3Key();
        if (rawKey == null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "processing"));
        }
        String processedKey = rawKey.replaceFirst("^raw/", "processed/");
        String errorKey = processedKey + ".error.json";

        try (ResponseInputStream<GetObjectResponse> obj = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(processedKey).build())) {
            byte[] body = obj.readAllBytes();
            return ResponseEntity.ok()
                    .header("X-Processed-Key", processedKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        } catch (NoSuchKeyException notProcessedYet) {
            return readErrorOrProcessing(errorKey);
        } catch (IOException ioe) {
            log.error("Error reading processed result from S3 (key={})", processedKey, ioe);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal server error."));
        } catch (Exception e) {
            log.error("Unexpected error retrieving processed result (key={})", processedKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal server error."));
        }
    }

    private ResponseEntity<?> readErrorOrProcessing(String errorKey) {
        try (ResponseInputStream<GetObjectResponse> err = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(errorKey).build())) {
            byte[] body = err.readAllBytes();
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        } catch (NoSuchKeyException stillProcessing) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "processing"));
        } catch (IOException ioe) {
            log.error("Error reading error file from S3 (key={})", errorKey, ioe);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal server error."));
        }
    }
}
