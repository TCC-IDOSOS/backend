package com.core.fisioaging.controller;


import com.core.fisioaging.service.impl.TestResultReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class TestResultReportController {

    private final TestResultReportService testResultReportService;

    @GetMapping("/test-results")
    public ResponseEntity<byte[]> generateTestResultsReport(
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            final LocalDate startDate,

            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            final LocalDate endDate,

            @RequestParam(value = "patientName", required = false)
            final String patientName,

            @RequestParam(value = "healthUnitId", required = false)
            final Long healthUnitId,

            @RequestParam(value = "patientAge", required = false)
            final Integer patientAge
    ) {
        byte[] pdf = testResultReportService.generatePdfReport(
                startDate,
                endDate,
                patientName,
                healthUnitId,
                patientAge
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("relatorio-resultados-testes.pdf")
                        .build()
        );

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(pdf);
    }
}
