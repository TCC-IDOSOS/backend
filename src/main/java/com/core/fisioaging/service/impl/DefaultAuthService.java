package com.core.fisioaging.service.impl;

import com.core.fisioaging.config.JwtService;
import com.core.fisioaging.data.LoginData;
import com.core.fisioaging.data.TokenData;
import com.core.fisioaging.exception.InvalidCredentialsException;
import com.core.fisioaging.model.UserModel;
import com.core.fisioaging.repository.UserRepository;
import com.core.fisioaging.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DefaultAuthService implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expiration;

    public DefaultAuthService(final UserRepository userRepository,
                              final PasswordEncoder passwordEncoder,
                              final JwtService jwtService,
                              @Value("${jwt.expiration}") final long expiration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.expiration = expiration;
    }

    @Override
    public TokenData login(final LoginData loginData) {
        final UserModel user = userRepository.findByEmail(loginData.getEmail())
                .filter(u -> passwordEncoder.matches(loginData.getPassword(), u.getPassword()))
                .orElseThrow(InvalidCredentialsException::new);

        return new TokenData(jwtService.generateToken(user.getEmail()), expiration, user.getId());
    }
}
