package com.core.fisioaging.service.impl;

import com.core.fisioaging.data.HealthUnitData;
import com.core.fisioaging.exception.ModelNotFoundException;
import com.core.fisioaging.mapper.HealthUnitMapper;
import com.core.fisioaging.model.HealthUnitModel;
import com.core.fisioaging.repository.HealthUnitRepository;
import com.core.fisioaging.service.HealthUnitService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DefaultHealthUnitService implements HealthUnitService {

    private final HealthUnitRepository healthUnitRepository;

    public DefaultHealthUnitService(final HealthUnitRepository healthUnitRepository) {
        this.healthUnitRepository = healthUnitRepository;
    }

    @Override
    public HealthUnitData create(final HealthUnitData healthUnitData) {
        final HealthUnitModel model = HealthUnitMapper.toModel(healthUnitData);
        model.setId(null);
        final HealthUnitModel savedModel = healthUnitRepository.save(model);
        return HealthUnitMapper.toData(savedModel);
    }

    @Override
    public HealthUnitData update(final Long id, final HealthUnitData healthUnitData) {
        final HealthUnitModel existingModel = healthUnitRepository.findById(id)
                .orElseThrow(() -> new ModelNotFoundException("Health unit not found for id: " + id));

        HealthUnitMapper.updateModel(existingModel, healthUnitData);
        existingModel.setId(id);

        final HealthUnitModel updatedModel = healthUnitRepository.save(existingModel);
        return HealthUnitMapper.toData(updatedModel);
    }

    @Override
    public HealthUnitData findById(final Long id) {
        return healthUnitRepository.findById(id)
                .map(HealthUnitMapper::toData)
                .orElseThrow(() -> new ModelNotFoundException("Health unit not found for id: " + id));
    }

    @Override
    public List<HealthUnitData> findAll() {
        return healthUnitRepository.findAll()
                .stream()
                .map(HealthUnitMapper::toData)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(final Long id) {
        if (!healthUnitRepository.existsById(id)) {
            throw new ModelNotFoundException("Health unit not found for id: " + id);
        }

        healthUnitRepository.deleteById(id);
    }
}