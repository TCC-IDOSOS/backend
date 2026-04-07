package com.core.fisioaging.config;

import com.core.fisioaging.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtFilter(final JwtService jwtService, final UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        final String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = header.substring(7);

        jwtService.extractEmail(token)
                .filter(email -> SecurityContextHolder.getContext().getAuthentication() == null)
                .flatMap(userRepository::findByEmail)
                .ifPresent(user -> {
                    final var auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });

        filterChain.doFilter(request, response);
    }
}
