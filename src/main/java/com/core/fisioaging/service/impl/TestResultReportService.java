package com.core.fisioaging.service.impl;

import com.core.fisioaging.model.TestResultModel;
import com.core.fisioaging.repository.TestResultRepository;
import com.core.fisioaging.repository.specification.TestResultSpecification;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestResultReportService {

    private final TestResultRepository testResultRepository;

    private static final String TEST_TYPE_UTT = "UTT";
    private static final String TEST_TYPE_MARCHA = "MARCHA";

    @Transactional(readOnly = true)
    public byte[] generatePdfReport(
            final LocalDate startDate,
            final LocalDate endDate,
            final String patientName,
            final Long healthUnitId,
            final Integer patientAge
    ) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<TestResultModel> results = testResultRepository.findAll(
                TestResultSpecification.filter(
                        startDateTime,
                        endDateTime,
                        patientName,
                        healthUnitId,
                        patientAge
                )
        );

        return buildPdf(results, startDate, endDate);
    }

    private byte[] buildPdf(
            final List<TestResultModel> results,
            final LocalDate startDate,
            final LocalDate endDate
    ) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            addTitle(document, startDate, endDate);
            addTable(document, results);

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório PDF de resultados de testes.", e);
        }
    }

    private void addTitle(
            final Document document,
            final LocalDate startDate,
            final LocalDate endDate
    ) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph title = new Paragraph("Relatório de Resultados de Testes", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(8);
        document.add(title);

        Paragraph period = new Paragraph(
                "Período: " + startDate + " até " + endDate,
                subtitleFont
        );
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(15);
        document.add(period);
    }

    private void addTable(
            final Document document,
            final List<TestResultModel> results
    ) throws DocumentException {
        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);

        table.setWidths(new float[]{
                2.5f, // nome
                1.0f, // idade
                1.2f, // sexo
                1.5f, // tipo
                1.8f, // data/hora
                2.3f, // unidade
                1.3f, // repeticoes
                1.3f, // altura media
                1.3f, // cadencia
                1.8f  // classificacao
        });

        addHeader(table, "Nome do paciente");
        addHeader(table, "Idade");
        addHeader(table, "Sexo");
        addHeader(table, "Tipo de teste");
        addHeader(table, "Data e hora");
        addHeader(table, "Unidade de saúde");
        addHeader(table, "Repetições");
        addHeader(table, "Altura média");
        addHeader(table, "Cadência");
        addHeader(table, "Classificação");

        if (results.isEmpty()) {
            addEmptyRow(table);
        } else {
            double averageUtt = averageRepetitions(results, TEST_TYPE_UTT);
            double averageMarcha = averageRepetitions(results, TEST_TYPE_MARCHA);

            for (TestResultModel result : results) {
                addCell(table, result.getPatientName());
                addCell(table, result.getPatientAge());
                addCell(table, result.getPatientSex() != null ? result.getPatientSex().getDescricao() : null);
                addCell(table, testTypeLabel(result.getTestType()));
                addCell(table, formatDateTime(result.getTestDateTime()));
                addCell(table, result.getHealthUnit() != null ? result.getHealthUnit().getName() : null);
                addCell(table, result.getTotalRepetitions());
                addCell(table, formatDecimal(result.getAverageHeight()));
                addCell(table, formatDecimal(result.getCadence()));
                addCell(table, classify(result, averageUtt, averageMarcha));
            }
        }

        document.add(table);
    }

    private double averageRepetitions(final List<TestResultModel> results, final String testType) {
        int sum = 0;
        int count = 0;

        for (TestResultModel result : results) {
            if (testType.equalsIgnoreCase(result.getTestType())) {
                sum += result.getTotalRepetitions() != null ? result.getTotalRepetitions() : 0;
                count++;
            }
        }

        return count > 0 ? (double) sum / count : 0;
    }

    /**
     * Classificação relativa à média do conjunto filtrado (mesmo critério do relatório no front):
     * acima de +10% da média -> "Acima da Média"; abaixo de -10% -> "Abaixo da Média"; senão "Na Média".
     */
    private String classify(
            final TestResultModel result,
            final double averageUtt,
            final double averageMarcha
    ) {
        int repetitions = result.getTotalRepetitions() != null ? result.getTotalRepetitions() : 0;

        double reference = TEST_TYPE_UTT.equalsIgnoreCase(result.getTestType())
                ? averageUtt
                : (TEST_TYPE_MARCHA.equalsIgnoreCase(result.getTestType()) ? averageMarcha : 0);

        if (reference > 0) {
            if (repetitions > reference * 1.1) {
                return "Acima da Média";
            }
            if (repetitions < reference * 0.9) {
                return "Abaixo da Média";
            }
            return "Na Média";
        }

        return result.getTestClassification() != null ? result.getTestClassification() : "Não Avaliado";
    }

    private String testTypeLabel(final String testType) {
        return TEST_TYPE_MARCHA.equalsIgnoreCase(testType) ? "Marcha Estacionária" : testType;
    }

    private void addHeader(final PdfPTable table, final String value) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Phrase phrase = new Phrase(value, font);
        table.addCell(phrase);
    }

    private void addCell(final PdfPTable table, final Object value) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8);
        String text = value == null ? "-" : String.valueOf(value);
        table.addCell(new Phrase(text, font));
    }

    private void addEmptyRow(final PdfPTable table) {
        addCell(table, "Nenhum resultado encontrado para os filtros informados.");
        for (int i = 1; i < 10; i++) {
            addCell(table, "");
        }
    }

    private String formatDateTime(final LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTime.format(formatter);
    }

    private String formatDecimal(final Double value) {
        if (value == null) {
            return "-";
        }

        return String.format("%.2f", value);
    }
}
