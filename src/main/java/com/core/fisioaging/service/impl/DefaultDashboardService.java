package com.core.fisioaging.service.impl;

import com.core.fisioaging.data.DashboardStatsData;
import com.core.fisioaging.enums.ProfileEnum;
import com.core.fisioaging.repository.TestExecutionRepository;
import com.core.fisioaging.repository.UserRepository;
import com.core.fisioaging.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DefaultDashboardService implements DashboardService {

    private final UserRepository userRepository;
    private final TestExecutionRepository testExecutionRepository;

    public DefaultDashboardService(final UserRepository userRepository,
                                   final TestExecutionRepository testExecutionRepository) {
        this.userRepository = userRepository;
        this.testExecutionRepository = testExecutionRepository;
    }

    @Override
    public DashboardStatsData getStats() {
        final DashboardStatsData stats = new DashboardStatsData();
        stats.setTotalPatients(userRepository.countByProfile(ProfileEnum.PACIENTE));
        stats.setTotalTests(testExecutionRepository.countByIsDeletedFalse());
        stats.setTestsByType(buildTestsByType());
        stats.setTestsLast30Days(testExecutionRepository.countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime.now().minusDays(30)));
        return stats;
    }

    private Map<String, Long> buildTestsByType() {
        final List<Object[]> rows = testExecutionRepository.countGroupByTestType();
        final Map<String, Long> result = new LinkedHashMap<>();
        for (final Object[] row : rows) {
            final String type = (String) row[0];
            final long count = ((Number) row[1]).longValue();
            result.put(type, count);
        }
        return result;
    }
}
