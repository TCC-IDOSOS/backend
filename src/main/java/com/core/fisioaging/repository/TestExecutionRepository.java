package com.core.fisioaging.repository;

import com.core.fisioaging.model.TestExecutionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TestExecutionRepository extends JpaRepository<TestExecutionModel, Long> {

    List<TestExecutionModel> findByUserEmailOrderByCreatedAtDesc(String email);

    @Query("""
        SELECT te
        FROM TestExecutionModel te
        WHERE te.rawS3Key IS NOT NULL
        AND NOT EXISTS (
            SELECT tr.id
            FROM TestResultModel tr
            WHERE tr.testExecution.id = te.id
        )
    """)
    List<TestExecutionModel> findExecutionsWithoutResult();
}
