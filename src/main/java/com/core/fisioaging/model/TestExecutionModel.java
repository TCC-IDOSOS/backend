package com.core.fisioaging.model;

import com.core.fisioaging.enums.GenreEnum;
import com.core.fisioaging.enums.TestExecutionStatusEnum;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "test_execution")
public class TestExecutionModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @Column(name = "test_type", nullable = false)
    private String testType;

    @Column(name = "test_date_time", nullable = false)
    private String testDateTime;

    @Column(name = "sensor")
    private String sensor;

    @Column(name = "frequency")
    private Integer frequency;

    @Column(name = "total_repetitions_app")
    private Integer totalRepetitionsApp;

    @Column(name = "raw_s3_key")
    private String rawS3Key;

    @Column(name = "processed_s3_key")
    private String processedS3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TestExecutionStatusEnum status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private GenreEnum genre;

    @Column(name = "age")
    private Integer age;

    @Column(name = "weight")
    private Double weight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_unit_id")
    private HealthUnitModel healthUnit;

}
