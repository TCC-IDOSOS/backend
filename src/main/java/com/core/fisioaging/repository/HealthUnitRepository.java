package com.core.fisioaging.repository;

import com.core.fisioaging.model.HealthUnitModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthUnitRepository extends JpaRepository<HealthUnitModel, Long> {

    Optional<HealthUnitModel> findByCnpj(String cnpj);
}
