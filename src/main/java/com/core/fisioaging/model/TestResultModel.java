package com.core.fisioaging.model;

import com.core.fisioaging.enums.GenreEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "test_result")
public class TestResultModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_execution_id", nullable = false)
    private TestExecutionModel testExecution;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "patient_age", nullable = false)
    private Integer patientAge;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_sex", nullable = false)
    private GenreEnum patientSex;

    @Column(name = "test_type", nullable = false)
    private String testType;

    @Column(name = "test_date_time", nullable = false)
    private LocalDateTime testDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_unit_id", nullable = false)
    private HealthUnitModel healthUnit;

    @Column(name = "total_repetitions")
    private Integer totalRepetitions;

    @Column(name = "average_height")
    private Double averageHeight;

    @Column(name = "cadence")
    private Double cadence;

    @Column(name = "test_classification")
    private String testClassification;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
