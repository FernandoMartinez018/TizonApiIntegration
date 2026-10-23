package com.lfmm.tizon_integration.service;

import com.lfmm.tizon_integration.dto.ErrorCode;
import com.lfmm.tizon_integration.dto.RegisterRequest;
import com.lfmm.tizon_integration.dto.UpdateUserRequest;
import com.lfmm.tizon_integration.dto.UserResponse;
import com.lfmm.tizon_integration.exception.ApiException;
import com.lfmm.tizon_integration.model.AppUser;
import com.lfmm.tizon_integration.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Registro, perfil y administración de usuarios (RF-01, RF-03). */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    /** Toda cuenta nueva queda como AGRICULTOR; solo un ADMIN cambia roles. */
    @Transactional
    public UserResponse register(RegisterRequest req) {
        if (users.existsByUsername(req.username())) throw usernameTaken();
        try {
            AppUser user = users.saveAndFlush(AppUser.builder()
                    .username(req.username())
                    .fullName(req.fullName().trim())
                    .passwordHash(passwordEncoder.encode(req.password()))
                    .role(AppUser.Role.AGRICULTOR)
                    .build());
            log.info("Usuario registrado username={}", user.getUsername());
            return UserResponse.of(user);
        } catch (DataIntegrityViolationException ex) {
            throw usernameTaken();   // registro simultáneo con el mismo usuario
        }
    }

    @Transactional(readOnly = true)
    public UserResponse me(String username) {
        return users.findByUsername(username).map(UserResponse::of)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                        "Debes iniciar sesión para continuar."));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAllByOrderByUsername().stream().map(UserResponse::of).toList();
    }

    @Transactional
    public UserResponse update(String adminUsername, UUID userId, UpdateUserRequest req) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "El usuario no existe."));
        if (user.getUsername().equals(adminUsername)) {
            // Evita que el último ADMIN se quite el rol o se deshabilite a sí mismo.
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.VALIDATION_ERROR,
                    "No puedes cambiar tu propio rol ni tu habilitación.");
        }
        if (req.role() != null) user.setRole(req.role());
        if (req.enabled() != null) user.setEnabled(req.enabled());
        log.info("Usuario actualizado username={} role={} enabled={} por={}",
                user.getUsername(), user.getRole(), user.isEnabled(), adminUsername);
        return UserResponse.of(user);
    }

    private static ApiException usernameTaken() {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.USERNAME_TAKEN, "Ese usuario ya existe. Elige otro.");
    }
}
