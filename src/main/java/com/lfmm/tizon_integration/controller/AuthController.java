package com.lfmm.tizon_integration.controller;

import com.lfmm.tizon_integration.dto.LoginRequest;
import com.lfmm.tizon_integration.dto.LoginResponse;
import com.lfmm.tizon_integration.dto.UserResponse;
import com.lfmm.tizon_integration.security.JwtTokenProvider;
import com.lfmm.tizon_integration.security.Roles;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // Credenciales inválidas → BadCredentialsException → 401 BAD_CREDENTIALS (GlobalExceptionHandler)
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        String token = tokenProvider.generateToken(auth.getName());
        log.info("Login exitoso username={}", auth.getName());
        return new LoginResponse(token, tokenProvider.getExpiration(token), toUser((UserDetails) auth.getPrincipal()));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserDetails user) {
        return toUser(user);
    }

    // shortcut: usuarios en memoria sin id ni nombre completo; F6-API-01 los reemplaza por la entidad User.
    private static UserResponse toUser(UserDetails user) {
        UUID id = UUID.nameUUIDFromBytes(user.getUsername().getBytes(StandardCharsets.UTF_8));
        return new UserResponse(id, user.getUsername(), user.getUsername(), Roles.primaryRole(user),
                user.isEnabled(), null);
    }
}
