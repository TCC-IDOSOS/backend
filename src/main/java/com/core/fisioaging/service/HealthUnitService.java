package com.core.fisioaging.service;

import com.core.fisioaging.data.HealthUnitData;

import java.util.List;

public interface HealthUnitService {

    HealthUnitData create(HealthUnitData healthUnitData);

    HealthUnitData update(Long id, HealthUnitData healthUnitData);

    HealthUnitData findById(Long id);

    List<HealthUnitData> findAll();

    void delete(Long id);
}