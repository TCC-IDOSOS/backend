package com.core.fisioaging.repository;

import com.core.fisioaging.model.HealthUnitModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthUnitRepository extends JpaRepository<HealthUnitModel, Long> {
}
