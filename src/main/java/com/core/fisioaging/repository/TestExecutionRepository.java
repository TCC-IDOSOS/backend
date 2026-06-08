package com.core.fisioaging.repository;

import com.core.fisioaging.model.TestExecutionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface TestExecutionRepository extends JpaRepository<TestExecutionModel, Long> {

    List<TestExecutionModel> findByUserEmailAndIsDeletedFalseOrderByCreatedAtDesc(String email);

    long countByIsDeletedFalse();

    long countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime since);

    @Query("SELECT t.testType, COUNT(t) FROM TestExecutionModel t WHERE t.isDeleted = false GROUP BY t.testType")
    List<Object[]> countGroupByTestType();

    @Query("""
        SELECT te
        FROM TestExecutionModel te
        WHERE te.rawS3Key IS NOT NULL
        AND te.isDeleted = false
        AND NOT EXISTS (
            SELECT tr.id
            FROM TestResultModel tr
            WHERE tr.testExecution.id = te.id
        )
    """)
    List<TestExecutionModel> findExecutionsWithoutResult();
}
