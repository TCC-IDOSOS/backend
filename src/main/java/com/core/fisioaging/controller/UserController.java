package com.core.fisioaging.controller;

import com.core.fisioaging.data.UserData;
import com.core.fisioaging.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "bearer")
public class UserController {

    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserData> create(@RequestBody final UserData userData) {
        final UserData createdUser = userService.create(userData);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserData> update(@PathVariable final Long id,
                                           @RequestBody final UserData userData) {
        final UserData updatedUser = userService.update(id, userData);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserData> getById(@PathVariable final Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserData>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
