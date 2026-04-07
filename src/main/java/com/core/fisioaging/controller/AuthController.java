package com.core.fisioaging.controller;

import com.core.fisioaging.data.LoginData;
import com.core.fisioaging.data.TokenData;
import com.core.fisioaging.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenData> login(@RequestBody final LoginData loginData) {
        return ResponseEntity.ok(authService.login(loginData));
    }
}
