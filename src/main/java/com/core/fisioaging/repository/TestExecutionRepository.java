package com.core.fisioaging.repository;

import com.core.fisioaging.model.TestExecutionModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestExecutionRepository extends JpaRepository<TestExecutionModel, Long> {
}
