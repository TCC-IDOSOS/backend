package com.core.fisioaging.controller;

import org.springframework.web.bind.annotation.RestController;

import com.core.fisioaging.data.UserData;
import com.core.fisioaging.facade.UserFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserFacade userFacade;

    public UserController(final UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @PostMapping
    public ResponseEntity<UserData> create(@RequestBody final UserData userData) {
        final UserData createdUser = userFacade.create(userData);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserData> update(@PathVariable final Long id,
                                           @RequestBody final UserData userData) {
        final UserData updatedUser = userFacade.update(id, userData);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserData> getById(@PathVariable final Long id) {
        return ResponseEntity.ok(userFacade.getById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserData>> getAll() {
        return ResponseEntity.ok(userFacade.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        userFacade.delete(id);
        return ResponseEntity.noContent().build();
    }
}
