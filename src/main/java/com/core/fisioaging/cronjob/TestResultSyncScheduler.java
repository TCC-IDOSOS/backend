package com.core.fisioaging.cronjob;

import com.core.fisioaging.service.impl.TestResultSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestResultSyncScheduler {

    private final TestResultSyncService testResultSyncService;

    @Scheduled(fixedDelayString = "${test-result.sync.fixed-delay-ms:300000}")
    public void syncTestResults() {
        log.info("Starting test result sync routine.");

        testResultSyncService.syncNewResultsFromS3();

        log.info("Finished test result sync routine.");
    }
}
