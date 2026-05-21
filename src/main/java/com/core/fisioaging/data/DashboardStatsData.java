package com.core.fisioaging.data;

import lombok.Data;

import java.util.Map;

@Data
public class DashboardStatsData {

    private long totalPatients;
    private long totalTests;
    private Map<String, Long> testsByType;
    private long testsLast30Days;
}
