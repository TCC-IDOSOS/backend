package com.core.fisioaging.repository;

import com.core.fisioaging.model.TestExecutionModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestExecutionRepository extends JpaRepository<TestExecutionModel, Long> {

    List<TestExecutionModel> findByUserEmailOrderByCreatedAtDesc(String email);
}
