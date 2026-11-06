package com.lfmm.tizon_integration.config;

import com.lfmm.tizon_integration.model.AppUser;
import com.lfmm.tizon_integration.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * RF-03 CA1: si no existe ningún ADMIN, lo crea con ADMIN_USERNAME y ADMIN_PASSWORD.
 * Si faltan esas variables, la API no arranca.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Value("${tizon.admin.username:}")
    private String adminUsername;

    @Value("${tizon.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole(AppUser.Role.ADMIN)) return;

        if (!StringUtils.hasText(adminUsername) || !StringUtils.hasText(adminPassword)) {
            throw new IllegalStateException(
                    "No existe ningún ADMIN: define ADMIN_USERNAME y ADMIN_PASSWORD para crearlo en el primer arranque");
        }
        if (adminPassword.length() < 8) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 8 caracteres");
        }
        if (users.existsByUsername(adminUsername)) {
            throw new IllegalStateException("ADMIN_USERNAME ya existe con otro rol; usa otro nombre");
        }
        users.save(AppUser.builder()
                .username(adminUsername)
                .fullName("Administrador")
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(AppUser.Role.ADMIN)
                .build());
        log.info("ADMIN inicial creado username={}", adminUsername);
    }
}
