package com.lfmm.tizon_integration.support;

import com.lfmm.tizon_integration.model.AppUser;
import com.lfmm.tizon_integration.repository.AppUserRepository;
import com.lfmm.tizon_integration.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

/** Crea usuarios únicos en la BD de pruebas (H2 compartida entre clases) y sus tokens. */
@TestComponent
@RequiredArgsConstructor
public class TestUsers {

    public static final String PASSWORD = "clave-de-prueba";

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AppUser create(AppUser.Role role) {
        String username = role.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8);
        return users.save(AppUser.builder()
                .username(username)
                .fullName("Usuario " + role.name())
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(role)
                .build());
    }

    public String bearer(AppUser user) {
        return "Bearer " + tokenProvider.generateToken(user.getUsername());
    }
}
