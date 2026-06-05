package com.core.fisioaging.service.impl;

import com.core.fisioaging.data.TestResultAwsResponse;
import com.core.fisioaging.model.TestExecutionModel;
import com.core.fisioaging.model.TestResultModel;
import com.core.fisioaging.repository.TestExecutionRepository;
import com.core.fisioaging.repository.TestResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestResultSyncService {

    private final TestExecutionRepository testExecutionRepository;
    private final TestResultRepository testResultRepository;
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    private static final String TEST_TYPE_MARCHA = "MARCHA";

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public void syncNewResultsFromS3() {
        List<Long> executionIds = testExecutionRepository.findExecutionsWithoutResult()
                .stream()
                .map(TestExecutionModel::getId)
                .toList();

        log.info("Found {} test executions without result.", executionIds.size());

        // Cada execução roda na própria transação: uma que falhe é descartada
        // (rollback isolado) sem derrubar o restante do sync.
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (Long executionId : executionIds) {
            try {
                transactionTemplate.executeWithoutResult(status -> processExecution(executionId));
            } catch (Exception e) {
                log.error("Error processing result for execution {}. Skipping.", executionId, e);
            }
        }
    }

    private void processExecution(final Long executionId) {
        TestExecutionModel execution = testExecutionRepository.findById(executionId).orElse(null);

        if (execution == null) {
            return;
        }

        if (testResultRepository.existsByTestExecutionId(execution.getId())) {
            return;
        }

        String rawKey = execution.getRawS3Key();

        if (rawKey == null || rawKey.isBlank()) {
            log.info("Test execution {} does not have rawS3Key yet.", execution.getId());
            return;
        }

        String processedKey = rawKey.replaceFirst("^raw/", "processed/");

        final TestResultAwsResponse awsResult;
        try {
            awsResult = readResultFromS3(processedKey);
        } catch (NoSuchKeyException e) {
            // Resultado ainda não processado: não é erro, tenta de novo no próximo ciclo.
            log.info(
                    "Processed result not found yet for execution {}. Key: {}",
                    execution.getId(),
                    processedKey
            );
            return;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read processed result from S3. Key: " + processedKey, e
            );
        }

        TestResultModel result = buildTestResult(execution, awsResult);

        testResultRepository.save(result);

        log.info(
                "Saved test result for execution {} from S3 key {}.",
                execution.getId(),
                processedKey
        );
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

        // A execução nem sempre tem unidade gravada; nesse caso usa a do usuário
        // (mesmo critério do relatório no front).
        result.setHealthUnit(
                execution.getHealthUnit() != null
                        ? execution.getHealthUnit()
                        : execution.getUser().getHealthUnit()
        );

        applyMetrics(result, execution, awsResult);
        result.setTestClassification(awsResult.getClassificacao());

        result.setCreatedAt(LocalDateTime.now());

        return result;
    }

    /**
     * Reproduz o cálculo que o relatório do front faz por teste:
     * - repetições: repeticoes_completas ?? n_peaks ?? peaks_t_s.length
     * - altura média: MARCHA usa a média dos picos (mediaZ); demais usam altura_media
     * - cadência: cadencia ?? cadence_cycles_min
     */
    private void applyMetrics(
            final TestResultModel result,
            final TestExecutionModel execution,
            final TestResultAwsResponse aws
    ) {
        boolean isMarcha = TEST_TYPE_MARCHA.equalsIgnoreCase(execution.getTestType());

        result.setTotalRepetitions(resolveRepetitions(aws));

        result.setAverageHeight(isMarcha ? computeMediaZ(aws) : aws.getAlturaMedia());

        result.setCadence(firstNonNull(aws.getCadencia(), aws.getCadenceCyclesMin()));
    }

    private Integer resolveRepetitions(final TestResultAwsResponse aws) {
        if (aws.getRepeticoesCompletas() != null) {
            return aws.getRepeticoesCompletas();
        }
        if (aws.getNPeaks() != null) {
            return aws.getNPeaks();
        }
        if (aws.getPeaksTS() != null) {
            return aws.getPeaksTS().size();
        }
        return 0;
    }

    private Double computeMediaZ(final TestResultAwsResponse aws) {
        List<Double> peaksZ;

        if (aws.getCycles() != null && !aws.getCycles().isEmpty()) {
            peaksZ = aws.getCycles().stream()
                    .map(c -> c.getWPhoneZPeakDegS() != null ? c.getWPhoneZPeakDegS() : 0.0)
                    .toList();
        } else {
            peaksZ = extractDoubles(aws.getPeaksValueDegS());
        }

        if (peaksZ.isEmpty()) {
            return 0.0;
        }

        double sum = peaksZ.stream().mapToDouble(v -> Math.abs(v != null ? v : 0.0)).sum();
        return sum / peaksZ.size();
    }

    /** peaks_value_deg_s pode ser um array JSON ou uma string contendo um array JSON. */
    private List<Double> extractDoubles(final JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }

        JsonNode array = node;
        if (node.isTextual()) {
            try {
                array = objectMapper.readTree(node.asText());
            } catch (Exception e) {
                return List.of();
            }
        }

        if (array == null || !array.isArray()) {
            return List.of();
        }

        List<Double> values = new ArrayList<>();
        array.forEach(n -> values.add(n.asDouble(0.0)));
        return values;
    }

    private Double firstNonNull(final Double first, final Double second) {
        return first != null ? first : second;
    }

    public static LocalDateTime parseTestDateTime(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        return LocalDateTime.parse(value, formatter);
    }
}
