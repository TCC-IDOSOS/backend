package com.core.fisioaging.controller;

import com.core.fisioaging.data.HealthUnitData;
import com.core.fisioaging.service.HealthUnitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/health-units")
public class HealthUnitController {

    private final HealthUnitService healthUnitService;

    public HealthUnitController(final HealthUnitService healthUnitService) {
        this.healthUnitService = healthUnitService;
    }

    @PostMapping
    public ResponseEntity<HealthUnitData> create(@RequestBody final HealthUnitData healthUnitData) {
        final HealthUnitData createdHealthUnit = healthUnitService.create(healthUnitData);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdHealthUnit);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HealthUnitData> update(@PathVariable final Long id,
                                                 @RequestBody final HealthUnitData healthUnitData) {
        final HealthUnitData updatedHealthUnit = healthUnitService.update(id, healthUnitData);
        return ResponseEntity.ok(updatedHealthUnit);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthUnitData> findById(@PathVariable final Long id) {
        final HealthUnitData healthUnitData = healthUnitService.findById(id);
        return ResponseEntity.ok(healthUnitData);
    }

    @GetMapping
    public ResponseEntity<List<HealthUnitData>> findAll() {
        final List<HealthUnitData> healthUnits = healthUnitService.findAll();
        return ResponseEntity.ok(healthUnits);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        healthUnitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}