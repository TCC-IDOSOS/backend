package com.core.fisioaging.repository;

import com.core.fisioaging.model.TestResultModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestResultRepository extends JpaRepository<TestResultModel, Long>, JpaSpecificationExecutor<TestResultModel> {

    boolean existsByTestExecutionId(Long testExecutionId);
}
